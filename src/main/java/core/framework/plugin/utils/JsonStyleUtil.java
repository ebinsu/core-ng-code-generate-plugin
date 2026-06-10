package core.framework.plugin.utils;

import com.fasterxml.jackson.core.util.DefaultIndenter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.core.util.Separators;

/**
 * @author ebin
 */
public class JsonStyleUtil {
    public static DefaultPrettyPrinter getCustomPrinter() {
        Separators baseSeparators = Separators.createDefaultInstance();

        Separators targetSeparators = baseSeparators.withObjectFieldValueSpacing(Separators.Spacing.AFTER);

        DefaultPrettyPrinter printer = new DefaultPrettyPrinter(targetSeparators);

        DefaultIndenter indenter = new DefaultIndenter("  ", DefaultIndenter.SYS_LF);
        printer.indentObjectsWith(indenter);
        printer.indentArraysWith(indenter);

        return printer;
    }
}
