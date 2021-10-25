package gm.tieba.tabswitch.hooker.eliminate;

import java.util.regex.Pattern;

import gm.tieba.tabswitch.dao.Preferences;

interface RegexFilter {
    String[] regex = new String[1];
    Pattern[] pattern = new Pattern[1];

    String key();

    default Pattern getPattern() {
        final var _regex = Preferences.getString(key());
        if (!_regex.equals(regex[0])) {
            regex[0] = _regex;
            pattern[0] = Pattern.compile(_regex);
        }
        return pattern[0];
    }
}
