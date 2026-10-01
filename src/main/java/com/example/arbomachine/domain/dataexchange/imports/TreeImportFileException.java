package com.example.arbomachine.domain.dataexchange.imports;

/** Problem z calym plikiem (format, kodowanie, brak kolumny...) - zamieniany na TreeImportIssue bez numeru wiersza. */
public class TreeImportFileException extends RuntimeException {
    private final TreeImportIssueCode code;
    private final String column;

    public TreeImportFileException(TreeImportIssueCode code) {
        this(code, null);
    }

    public TreeImportFileException(TreeImportIssueCode code, String column) {
        super("Tree import file rejected: " + code);
        this.code = code;
        this.column = column;
    }

    public TreeImportIssue toIssue() {
        return TreeImportIssue.ofFile(code, column);
    }
}
