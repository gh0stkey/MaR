package mar.utils.rule.model;

import java.util.LinkedHashMap;
import java.util.Map;

public class Info {
    private Map<String, Object> fields;

    public Info(boolean loaded, String name, String c_scope, String relationship, String condition, boolean c_regex, String m_scope, String match, String replace, boolean m_regex) {
        fields = new LinkedHashMap<>();
        fields.put("name", name);
        fields.put("loaded", loaded);
        fields.put("c_scope", c_scope);
        fields.put("relationship", relationship);
        fields.put("condition", condition);
        fields.put("c_regex", c_regex);
        fields.put("m_scope", m_scope);
        fields.put("match", match);
        fields.put("replace", replace);
        fields.put("m_regex", m_regex);
    }

    public Map<String, Object> getFields() {
        return fields;
    }

    public void loadFields(Map<String, Object> fields) {
        this.fields = fields;
    }
}