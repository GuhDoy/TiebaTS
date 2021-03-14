package gm.tieba.tabswitch.util;

import java.lang.reflect.Field;
import java.util.List;

import gm.tieba.tabswitch.Hook;

public class TbProtoParser extends Hook {
    public static String pbContentParser(Object instance, String fieldName) throws NoSuchFieldException, IllegalAccessException {
        Field content = instance.getClass().getDeclaredField(fieldName);
        content.setAccessible(true);
        List<?> contents = (List<?>) content.get(instance);
        StringBuilder pbContent = new StringBuilder();
        for (int j = 0; j < contents.size(); j++) {
            Field text = contents.get(j).getClass().getDeclaredField("text");
            text.setAccessible(true);
            pbContent.append(text.get(contents.get(j)));
        }
        return pbContent.toString();
    }
}
