package com.smsmurilo.notify;

import com.smsmurilo.notify.domain.TemplateRenderer;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TemplateRendererTest {

    private final TemplateRenderer renderer = new TemplateRenderer();

    @Test
    void rendersVariables() {
        var text = renderer.render("consulta-confirmada",
                Map.of("nome", "Ana", "especialidade", "Cardiologia", "data", "12/11 às 9h"));
        assertThat(text).isEqualTo("Olá Ana, sua consulta com Cardiologia está confirmada para 12/11 às 9h.");
    }

    @Test
    void failsFastOnMissingVariableOrUnknownTemplate() {
        assertThatThrownBy(() -> renderer.render("codigo-acesso", Map.of())).hasMessageContaining("codigo");
        assertThatThrownBy(() -> renderer.render("nao-existe", Map.of())).hasMessageContaining("desconhecido");
    }
}
