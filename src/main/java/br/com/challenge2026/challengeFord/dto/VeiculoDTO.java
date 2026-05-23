package br.com.challenge2026.challengeFord.dto;

import br.com.challenge2026.challengeFord.model.Especificacoes;

import java.util.List;

public record VeiculoDTO(
        String marca,
        String modelo,
        String versao,
        List<Especificacoes> especificacoesList
) {
}
