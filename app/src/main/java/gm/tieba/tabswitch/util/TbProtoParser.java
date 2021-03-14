package gm.tieba.tabswitch.util;

import gm.tieba.tabswitch.Hook;

public class TbProtoParser extends Hook {
    public static class ThreadInfoParser {
        public String tid;// not for the time being
        public String title;
        public String fname;
        public String firstPostContent;
        public String name;
        public String nameShow;

        public ThreadInfoParser(String threadInfo) throws StringIndexOutOfBoundsException {
            tid = threadInfo.substring(threadInfo.indexOf(", tid=") + 6, threadInfo.indexOf(", tieba_game_information_source="));
            title = threadInfo.substring(threadInfo.indexOf(", title=") + 8, threadInfo.indexOf(", topic_h5_url="));
            fname = threadInfo.substring(threadInfo.indexOf(", fname=") + 8, threadInfo.indexOf(", forum_info="));
            firstPostContent = pbContentParser(threadInfo.substring(threadInfo.indexOf(", first_post_content=[") + 22, threadInfo.indexOf("], first_post_id=")));
            String author = threadInfo.substring(threadInfo.indexOf(", author=") + 9, threadInfo.indexOf(", author_id="));
            name = author.substring(author.indexOf(", name=") + 7, author.indexOf(", name_show="));
            nameShow = author.substring(author.indexOf(", name_show=") + 12, author.indexOf(", new_god_data="));
        }
    }

    public static class SubPostParser {
        public String content;
        public String name;
        public String nameShow;

        public SubPostParser(String subPost) throws StringIndexOutOfBoundsException {
            content = pbContentParser(subPost.substring(subPost.indexOf(", content=[") + 11, subPost.indexOf("], floor=")));
            String author = subPost.substring(subPost.indexOf(", author=") + 9, subPost.indexOf(", author_id="));
            name = author.substring(author.indexOf(", name=") + 7, author.indexOf(", name_show="));
            nameShow = author.substring(author.indexOf(", name_show=") + 12, author.indexOf(", new_god_data="));
        }
    }

    private static String pbContentParser(String pbContents) throws StringIndexOutOfBoundsException {
        StringBuilder pbContentBuilder = new StringBuilder();
        for (String pbContent : pbContents.split(", PbContent"))
            pbContentBuilder.append(pbContent.substring(pbContent.indexOf(", text=") + 7, pbContent.indexOf(", topic_special_icon=")));
        return pbContentBuilder.toString();
    }
}