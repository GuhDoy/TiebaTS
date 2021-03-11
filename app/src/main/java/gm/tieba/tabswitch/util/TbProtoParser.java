package gm.tieba.tabswitch.util;

import gm.tieba.tabswitch.Hook;

public class TbProtoParser extends Hook {
    public static class ThreadInfoParser {
        public String tid;// not for the time being
        public String title;
        public String fname;
        public String pbContent;
        public String name;
        public String name_show;

        public ThreadInfoParser(String threadInfo) throws StringIndexOutOfBoundsException {
            tid = threadInfo.substring(threadInfo.indexOf(", tid=") + 6, threadInfo.indexOf(", tieba_game_information_source="));
            title = threadInfo.substring(threadInfo.indexOf(", title=") + 8, threadInfo.indexOf(", topic_h5_url="));
            fname = threadInfo.substring(threadInfo.indexOf(", fname=") + 8, threadInfo.indexOf(", forum_info="));

            String firstPostContent = threadInfo.substring(threadInfo.indexOf(", first_post_content=[") + 22, threadInfo.indexOf("], first_post_id="));
            StringBuilder pbContentBuilder = new StringBuilder();
            String[] pbContents = firstPostContent.split(", PbContent");
            for (String content : pbContents)
                pbContentBuilder.append(content.substring(content.indexOf(", text=") + 7, content.indexOf(", topic_special_icon=")));
            pbContent = pbContentBuilder.toString();

            String author = threadInfo.substring(threadInfo.indexOf(", author=") + 9, threadInfo.indexOf(", author_id="));
            name = author.substring(author.indexOf(", name=") + 7, author.indexOf(", name_show="));
            name_show = author.substring(author.indexOf(", name_show=") + 12, author.indexOf(", new_god_data="));
        }
    }
}
