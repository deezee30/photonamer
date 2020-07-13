/*
 * Part of photonamer.
 *
 * @author deezee30 (2020).
 */

package me.deezee.photonamer.ui;

import javafx.util.Callback;
import javafx.util.StringConverter;
import me.deezee.photonamer.format.NamerFormat;
import me.deezee.photonamer.PhotoNamer;
import org.apache.commons.lang3.Validate;
import org.controlsfx.control.textfield.AutoCompletionBinding;
import org.controlsfx.control.textfield.TextFields;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class VarTextCompletionCaller
        implements Callback<AutoCompletionBinding.ISuggestionRequest, Collection<String>> {

    @Override
    public Collection<String> call(AutoCompletionBinding.ISuggestionRequest suggRequest) {
        String typed = suggRequest.getUserText();

        // Obtain current variable name. The potential variable name has criteria such that it
        // begins with a dollar sign and does not include spaces
        int last$ = typed.lastIndexOf("$");

        // Do not make suggestions to non-variables
        if (last$ == -1) return Collections.emptyList();

        // Do not include $ in variable name
        String typedVar = typed.substring(last$ + 1);

        // Do not make suggestions to non-variables
        if (typedVar.contains(" ")) return Collections.emptyList();

        // Filter down to possible variables
        NamerFormat.Var[] vars = NamerFormat.Var.values();
        List<String> suggestions = new ArrayList<>(vars.length);

        for (NamerFormat.Var var : vars)
            if (var.getName().toLowerCase().contains(typedVar.toLowerCase()))
                suggestions.add(var.getVariable());

        return suggestions;
    }

    public static AutoCompletionBinding<String> createVarCompleter(PhotoNamer.StartApplication appInstance) {
        AutoCompletionBinding<String> acb = TextFields.bindAutoCompletion(
                appInstance.getFormatField(),
                new VarTextCompletionCaller(),
                new Converter(appInstance)
        );

        acb.setDelay(50);

        return acb;
    }

    public static class Converter extends StringConverter<String> {

        private final PhotoNamer.StartApplication appInstance;

        private Converter(PhotoNamer.StartApplication appInstance) {
            this.appInstance = Validate.notNull(appInstance);
        }

        @Override
        public String toString(String select) {
            String current = appInstance.getFormatField().getText();
            int last$ = current.lastIndexOf("$");

            // Error; escape
            if (last$ == -1) return current;

            // Replace (un)finished placeholder with selected variable
            return current.substring(0, last$) + select;
        }

        @Override
        public String fromString(String string) {
            return string;
        }
    }
}