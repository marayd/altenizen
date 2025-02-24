package com.marayd.denizenImplementation.DenizenHook;

import com.denizenscript.denizencore.objects.core.ElementTag;
import com.marayd.denizenImplementation.DenizenImplementation;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

public class ElemenTagProc {
    private static final DenizenImplementation plugin = DenizenImplementation.instance;

    public static void start() {
        if (ElementTag.tagProcessor != null) {
            ElementTag.tagProcessor.registerTag(ElementTag.class, "html_get_text", ((attribute, elementTag) -> {
                Document doc = Jsoup.parse(attribute.toString());
                Element bodyContent = doc.getElementById("bodyContent");
                if (bodyContent != null) {
                    String text = bodyContent.text();
                    return new ElementTag(text);
                }
                return null;
            }));
        }
        else {
            plugin.getLogger().severe("Entity TagProcessor is null.");
        }

    }
}
