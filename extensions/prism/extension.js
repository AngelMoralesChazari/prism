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

    // Eventos del editor
    context.subscriptions.push(
        vscode.workspace.onDidOpenTextDocument(doc => runDiagnostics(doc)),
        vscode.workspace.onDidSaveTextDocument(doc => runDiagnostics(doc)),
        vscode.workspace.onDidChangeTextDocument(event => scheduleCheck(event.document)),
        vscode.workspace.onDidCloseTextDocument(doc => diagnosticCollection.delete(doc.uri))
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

