package core.framework.plugin.api.link;

import com.intellij.ide.BrowserUtil;
import com.intellij.openapi.ui.popup.PopupStep;
import com.intellij.openapi.ui.popup.util.BaseListPopupStep;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * @author ebin
 */
public class KibanaPopupStep extends BaseListPopupStep<KibanaLink> {
    public KibanaPopupStep(String serviceName, String action) {
        KibanaLink qa = new KibanaLink("qa", serviceName, action);
        KibanaLink uat = new KibanaLink("uat", serviceName, action);
        KibanaLink prod = new KibanaLink("prod", serviceName, action);
        List<KibanaLink> kibanaLinks = List.of(qa, uat, prod);
        init("Select Env :", kibanaLinks, null);
    }

    @Override
    public @NotNull String getTextFor(KibanaLink value) {
        return value.env;
    }

    @Override
    public @Nullable PopupStep<?> onChosen(KibanaLink selectedValue, boolean finalChoice) {
        BrowserUtil.browse(selectedValue.url);
        return super.onChosen(selectedValue, finalChoice);
    }
}
