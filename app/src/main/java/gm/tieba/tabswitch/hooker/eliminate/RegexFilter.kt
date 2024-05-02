package gm.tieba.tabswitch.hooker.eliminate

import gm.tieba.tabswitch.dao.Preferences.getString
import java.util.regex.Pattern

internal interface RegexFilter {
    fun key(): String
    fun getPattern(): Pattern? {
        val _regex = getString(key()) ?: return null
        if (_regex != regex[0]) {
            regex[0] = _regex
            pattern[0] = Pattern.compile(_regex, Pattern.CASE_INSENSITIVE)
        }
        return pattern[0]
    }

    companion object {
        val regex = arrayOfNulls<String>(1)
        val pattern = arrayOfNulls<Pattern>(1)
    }
}
