package com.smsmurilo.notify.domain;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class TemplateRenderer {

    private static final Pattern PLACEHOLDER = Pattern.compile("\\{\\{\\s*(\\w+)\\s*}}");

    private final Map<String, String> templates = Map.of(
            "consulta-confirmada", "Olá {{nome}}, sua consulta com {{especialidade}} está confirmada para {{data}}.",
            "codigo-acesso", "Seu código de acesso é {{codigo}}. Não compartilhe com ninguém.",
            "protocolo-atualizado", "Protocolo {{protocolo}} atualizado para: {{status}}.");

    public String render(String templateId, Map<String, String> variables) {
        var template = templates.get(templateId);
        if (template == null) {
            throw new IllegalArgumentException("Template desconhecido: " + templateId);
        }
        Matcher m = PLACEHOLDER.matcher(template);
        var out = new StringBuilder();
        while (m.find()) {
            var value = variables.get(m.group(1));
            if (value == null) {
                throw new IllegalArgumentException("Variável ausente: " + m.group(1));
            }
            m.appendReplacement(out, Matcher.quoteReplacement(value));
        }
        return m.appendTail(out).toString();
    }
}
