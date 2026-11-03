const vscode = require('vscode');
const { execFile } = require('child_process');
const path = require('path');
const fs = require('fs');
const os = require('os');

/**
 * Extensión oficial de Prism para VS Code / Cursor.
 * Proporciona diagnósticos visuales en tiempo real (errores de sintaxis en rojo,
 * avisos semánticos en amarillo) y acciones rápidas ("Quick Fix" con el foco).
 */
function activate(context) {
    const diagnosticCollection = vscode.languages.createDiagnosticCollection('prism');
    context.subscriptions.push(diagnosticCollection);

    let debounceTimer = null;

    function getJarPath() {
        // 1. Buscar en bin/ de la extensión
        const bundled = context.asAbsolutePath('bin/prism.jar');
        if (fs.existsSync(bundled)) {
            return bundled;
        }
        // 2. Buscar en la raíz del espacio de trabajo abierto
        if (vscode.workspace.workspaceFolders && vscode.workspace.workspaceFolders.length > 0) {
            const rootJar = path.join(vscode.workspace.workspaceFolders[0].uri.fsPath, 'prism.jar');
            if (fs.existsSync(rootJar)) {
                return rootJar;
            }
        }
        return 'prism.jar';
    }

    function runDiagnostics(document) {
        if (!document || (document.languageId !== 'prism' && !document.fileName.endsWith('.pr'))) {
            return;
        }

        const jarPath = getJarPath();
        const text = document.getText();
        if (!text.trim()) {
            diagnosticCollection.delete(document.uri);
            return;
        }

        // Crear carpeta y archivo temporal preservando el nombre real del archivo (p. ej. test.pr)
        const realBasename = path.basename(document.fileName || 'document.pr');
        const tempDir = path.join(os.tmpdir(), `prism_check_${process.pid}_${Date.now()}_${Math.random().toString(36).substring(2, 6)}`);
        const tempFile = path.join(tempDir, realBasename);
        try {
            fs.mkdirSync(tempDir, { recursive: true });
            fs.writeFileSync(tempFile, text, 'utf-8');
        } catch (e) {
            return;
        }

        execFile('java', ['-jar', jarPath, 'check', '--json', tempFile], (error, stdout, stderr) => {
            // Eliminar archivo temporal y su directorio
            try {
                if (fs.existsSync(tempDir)) {
                    fs.rmSync(tempDir, { recursive: true, force: true });
                }
            } catch (_) {}

            if (!stdout || !stdout.trim().startsWith('[')) {
                // Si no hay salida JSON válida
                if (stderr && stderr.trim()) {
                    // Fallback para mensajes de error de texto plano
                    const match = stderr.match(/línea\s+(\d+)(?::(\d+))?/i);
                    const line = match ? Math.max(0, parseInt(match[1], 10) - 1) : 0;
                    const col = match && match[2] ? Math.max(0, parseInt(match[2], 10) - 1) : 0;
                    const range = new vscode.Range(line, col, line, col + 10);
                    const diag = new vscode.Diagnostic(range, stderr.trim(), vscode.DiagnosticSeverity.Error);
                    diag.source = 'Prism';
                    diagnosticCollection.set(document.uri, [diag]);
                } else {
                    diagnosticCollection.delete(document.uri);
                }
                return;
            }

            try {
                const items = JSON.parse(stdout);
                const diags = items.map(item => {
                    const line = Math.max(0, (item.line || 1) - 1);
                    const col = Math.max(0, (item.col || 1) - 1);
                    const endLine = Math.max(0, (item.endLine || item.line || 1) - 1);
                    const endCol = Math.max(col + 1, item.endCol ? item.endCol - 1 : col + 1);

                    const range = new vscode.Range(line, col, endLine, endCol);
                    const severity = item.severity === 'error'
                        ? vscode.DiagnosticSeverity.Error
                        : vscode.DiagnosticSeverity.Warning;

                    const diagnostic = new vscode.Diagnostic(range, item.message, severity);
                    diagnostic.source = 'Prism';
                    if (item.suggestion) {
                        diagnostic.code = item.suggestion;
                    }
                    return diagnostic;
                });

                diagnosticCollection.set(document.uri, diags);
            } catch (parseErr) {
                diagnosticCollection.delete(document.uri);
            }
        });
    }

    function scheduleCheck(document) {
        if (debounceTimer) {
            clearTimeout(debounceTimer);
        }
        debounceTimer = setTimeout(() => {
            runDiagnostics(document);
        }, 350);
    }

    /**
     * Determina la indentación necesaria para líneas dentro de cadenas multilínea
     * como 'nl premisa = "...'. Aplica 1 solo tab (sangría estándar de 4 espacios)
     * respecto al inicio de la declaración.
     */
    function findStringIndent(lines, targetLineIndex, indentUnit = '    ') {
        let totalQuotes = 0;
        let declIndent = null;
        let foundDecl = false;

        for (let i = targetLineIndex; i >= 0; i--) {
            const line = lines[i];
            const quotesOnLine = (line.match(/(?<!\\)"/g) || []).length;
            totalQuotes += quotesOnLine;

            const m = line.match(/^(\s*)(?:nl|atom|let|formula)\s+[\w.]+\s*=\s*"/);
            if (m) {
                const baseIndent = m[1];
                declIndent = baseIndent + indentUnit;
                foundDecl = true;
                break;
            }
        }

        if (foundDecl && totalQuotes % 2 !== 0) {
            return declIndent;
        }
        return null;
    }

    let isFormattingIndent = false;

    // Eventos del editor
    context.subscriptions.push(
        vscode.workspace.onDidOpenTextDocument(doc => runDiagnostics(doc)),
        vscode.workspace.onDidSaveTextDocument(doc => runDiagnostics(doc)),
        vscode.workspace.onDidChangeTextDocument(async (event) => {
            scheduleCheck(event.document);

            // Auto-alineación al presionar Enter dentro de premisas multilínea (1 tab)
            if (isFormattingIndent || !event.document || (event.document.languageId !== 'prism' && !event.document.fileName.endsWith('.pr'))) {
                return;
            }

            const editor = vscode.window.activeTextEditor;
            if (!editor || editor.document !== event.document) {
                return;
            }

            for (const change of event.contentChanges) {
                if (change.text && (change.text.includes('\n') || change.text.includes('\r'))) {
                    const splitLineIndex = change.range.start.line;
                    const doc = event.document;
                    if (splitLineIndex + 1 >= doc.lineCount) {
                        continue;
                    }

                    const lines = [];
                    for (let i = 0; i <= splitLineIndex; i++) {
                        lines.push(doc.lineAt(i).text);
                    }

                    const indentUnit = (editor && editor.options.insertSpaces === false) ? '\t' : '    ';
                    const targetIndent = findStringIndent(lines, splitLineIndex, indentUnit);
                    if (targetIndent !== null) {
                        const nextLine = doc.lineAt(splitLineIndex + 1);
                        const currentLeading = nextLine.text.match(/^\s*/)[0];

                        if (currentLeading !== targetIndent) {
                            isFormattingIndent = true;
                            try {
                                const replaceRange = new vscode.Range(
                                    splitLineIndex + 1, 0,
                                    splitLineIndex + 1, currentLeading.length
                                );
                                await editor.edit(editBuilder => {
                                    editBuilder.replace(replaceRange, targetIndent);
                                }, { undoStopBefore: false, undoStopAfter: false });

                                const newPos = new vscode.Position(splitLineIndex + 1, targetIndent.length);
                                editor.selection = new vscode.Selection(newPos, newPos);
                            } catch (_) {}
                            finally {
                                isFormattingIndent = false;
                            }
                        }
                    }
                }
            }
        }),
        vscode.workspace.onDidCloseTextDocument(doc => diagnosticCollection.delete(doc.uri))
    );

    // Formateador de documento (Shift + Alt + F) para alinear premisas multilínea con 1 tab
    context.subscriptions.push(
        vscode.languages.registerDocumentFormattingEditProvider('prism', {
            provideDocumentFormattingEdits(document) {
                const edits = [];
                const lineCount = document.lineCount;
                let inString = false;
                let indentSpaces = '';

                for (let i = 0; i < lineCount; i++) {
                    const line = document.lineAt(i);
                    const text = line.text;

                    if (!inString) {
                        const m = text.match(/^(\s*)(?:nl|atom|let|formula)\s+[\w.]+\s*=\s*"/);
                        if (m) {
                            const quotes = (text.match(/(?<!\\)"/g) || []).length;
                            if (quotes % 2 !== 0) {
                                inString = true;
                                indentSpaces = m[1] + '    ';
                            }
                        }
                    } else {
                        const currentLeading = text.match(/^\s*/)[0];
                        if (currentLeading !== indentSpaces && text.trim().length > 0) {
                            const range = new vscode.Range(i, 0, i, currentLeading.length);
                            edits.push(vscode.TextEdit.replace(range, indentSpaces));
                        }
                        const quotes = (text.match(/(?<!\\)"/g) || []).length;
                        if (quotes % 2 !== 0) {
                            inString = false;
                        }
                    }
                }
                return edits;
            }
        })
    );

    // Proveedor de acciones rápidas (Quick Fix / Foquito 💡)
    const codeActionProvider = {
        provideCodeActions(document, range, context) {
            const actions = [];
            for (const diag of context.diagnostics) {
                if (diag.source === 'Prism' && diag.code) {
                    const suggestion = String(diag.code);
                    const isFullStatement = suggestion.startsWith('atom ');
                    const title = isFullStatement
                        ? `Sustituir por: ${suggestion}`
                        : `Corregir a '${suggestion}'`;

                    const action = new vscode.CodeAction(title, vscode.CodeActionKind.QuickFix);
                    action.diagnostics = [diag];
                    action.isPreferred = true;

                    const edit = new vscode.WorkspaceEdit();
                    edit.replace(document.uri, diag.range, suggestion);
                    action.edit = edit;
                    actions.push(action);
                }
            }
            return actions;
        }
    };

    context.subscriptions.push(
        vscode.languages.registerCodeActionsProvider('prism', codeActionProvider, {
            providedCodeActionKinds: [vscode.CodeActionKind.QuickFix]
        })
    );

    // Comando universal para alternar comentarios de bloque /* ... */ (sin conflicto con atajos de Linux)
    context.subscriptions.push(
        vscode.commands.registerCommand('prism.toggleBlockComment', () => {
            const editor = vscode.window.activeTextEditor;
            if (!editor) {
                return;
            }

            const selection = editor.selection;
            const doc = editor.document;
            const selectedText = doc.getText(selection);

            // Si no hay texto seleccionado, insertar /*  */ y colocar el cursor en medio
            if (!selectedText) {
                editor.edit(editBuilder => {
                    editBuilder.insert(selection.active, '/*  */');
                }).then(() => {
                    const newPos = selection.active.translate(0, 3);
                    editor.selection = new vscode.Selection(newPos, newPos);
                });
                return;
            }

            const trimmed = selectedText.trim();
            // Si ya está envuelto en /* ... */, descomentar
            if (trimmed.startsWith('/*') && trimmed.endsWith('*/')) {
                const unwrapped = trimmed.substring(2, trimmed.length - 2).trim();
                editor.edit(editBuilder => {
                    editBuilder.replace(selection, unwrapped);
                });
            } else {
                // Comentar con /* ... */
                const isMultiline = selectedText.includes('\n');
                const commentText = isMultiline
                    ? `/*\n  ${selectedText.split('\n').join('\n  ')}\n*/`
                    : `/* ${selectedText} */`;
                editor.edit(editBuilder => {
                    editBuilder.replace(selection, commentText);
                });
            }
        })
    );

    // Ejecutar diagnóstico inicial sobre los documentos visibles
    if (vscode.window.activeTextEditor) {
        runDiagnostics(vscode.window.activeTextEditor.document);
    }
}

function deactivate() {}

module.exports = {
    activate,
    deactivate
};

