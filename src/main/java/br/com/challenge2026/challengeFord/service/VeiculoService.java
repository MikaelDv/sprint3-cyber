package br.com.challenge2026.challengeFord.service;

import br.com.challenge2026.challengeFord.dto.VeiculoDTO;
import br.com.challenge2026.challengeFord.model.Especificacoes;
import br.com.challenge2026.challengeFord.model.Veiculo;
import br.com.challenge2026.challengeFord.repository.VeiculoRepository;
import br.com.challenge2026.challengeFord.util.InputSanitizer;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class VeiculoService {

    @Autowired
    VeiculoRepository repository;

    @Autowired
    GeminiService geminiService;

    @Autowired
    AuditService auditService;

    public Page<VeiculoDTO> listarVeiculos(Pageable pageable, HttpServletRequest request) {
        Page<VeiculoDTO> page = repository.findAll(pageable)
                .map(v -> new VeiculoDTO(v.getMarca(), v.getModelo(), v.getVersao(), v.getEspecificacoesList()));
        if (page.getTotalElements() > 200) {
            auditService.registrar("CONSULTA_MASSIVA", "SUCESSO", "/veiculos",
                    "total=" + page.getTotalElements(), request);
        }
        return page;
    }

    public VeiculoDTO buscarVeiculo(String marca, String modelo, String versao) {
        String safeMarca = InputSanitizer.normalize(marca);
        String safeModelo = InputSanitizer.normalize(modelo);
        String safeVersao = InputSanitizer.normalize(versao);
        InputSanitizer.rejectIfMalicious("marca", safeMarca);
        InputSanitizer.rejectIfMalicious("modelo", safeModelo);
        InputSanitizer.rejectIfMalicious("versao", safeVersao);

        return repository.findByMarcaAndModeloAndVersao(safeMarca, safeModelo, safeVersao)
                .map(v -> new VeiculoDTO(v.getMarca(), v.getModelo(), v.getVersao(), v.getEspecificacoesList()))
                .orElseThrow(() -> new EntityNotFoundException("Veículo não encontrado"));
    }

    @Transactional
    public VeiculoDTO salvarVeiculo(Veiculo veiculo, HttpServletRequest request) {
        veiculo.setMarca(InputSanitizer.normalize(veiculo.getMarca()));
        veiculo.setModelo(InputSanitizer.normalize(veiculo.getModelo()));
        veiculo.setVersao(InputSanitizer.normalize(veiculo.getVersao()));

        if (veiculo.getMarca() == null || veiculo.getMarca().isBlank()
                || veiculo.getModelo() == null || veiculo.getModelo().isBlank()
                || veiculo.getVersao() == null || veiculo.getVersao().isBlank()) {
            throw new IllegalArgumentException("Marca, modelo e versão são obrigatórios");
        }
        InputSanitizer.rejectIfMalicious("marca", veiculo.getMarca());
        InputSanitizer.rejectIfMalicious("modelo", veiculo.getModelo());
        InputSanitizer.rejectIfMalicious("versao", veiculo.getVersao());

        if (veiculo.getEspecificacoesList() == null) {
            veiculo.setEspecificacoesList(new ArrayList<>());
        } else {
            for (Especificacoes e : veiculo.getEspecificacoesList()) {
                e.setNome(InputSanitizer.normalize(e.getNome()));
                e.setValor(InputSanitizer.normalize(e.getValor()));
            }
        }

        if (repository.findByMarcaAndModeloAndVersao(veiculo.getMarca(), veiculo.getModelo(), veiculo.getVersao()).isPresent()) {
            throw new IllegalArgumentException("Veículo já está salvo no banco");
        }

        Veiculo salvo = repository.save(veiculo);
        auditService.registrar("CRIAR_VEICULO", "SUCESSO", "/veiculos",
                "id=" + salvo.getId(), request);
        return new VeiculoDTO(salvo.getMarca(), salvo.getModelo(), salvo.getVersao(), salvo.getEspecificacoesList());
    }

    @Transactional
    public List<Especificacoes> consultarVeiculo(String marca,
                                                 String modelo,
                                                 String versao,
                                                 String prompt,
                                                 HttpServletRequest request) {
        String safeMarca = InputSanitizer.normalize(marca);
        String safeModelo = InputSanitizer.normalize(modelo);
        String safeVersao = InputSanitizer.normalize(versao);
        InputSanitizer.rejectIfMalicious("marca", safeMarca);
        InputSanitizer.rejectIfMalicious("modelo", safeModelo);
        InputSanitizer.rejectIfMalicious("versao", safeVersao);

        List<Especificacoes> novasSpecs =
                geminiService.gerarEspecificacoes(safeMarca, safeModelo, safeVersao, prompt);

        Optional<Veiculo> optional =
                repository.findByMarcaAndModeloAndVersao(safeMarca, safeModelo, safeVersao);

        Veiculo veiculo;
        if (optional.isPresent()) {
            veiculo = optional.get();
        } else {
            veiculo = new Veiculo();
            veiculo.setMarca(safeMarca);
            veiculo.setModelo(safeModelo);
            veiculo.setVersao(safeVersao);
            veiculo.setEspecificacoesList(new ArrayList<>());
        }

        for (Especificacoes nova : novasSpecs) {
            boolean existe = veiculo.getEspecificacoesList().stream()
                    .anyMatch(e -> e.getNome() != null
                            && e.getNome().equalsIgnoreCase(nova.getNome()));
            if (!existe) {
                veiculo.getEspecificacoesList().add(nova);
            }
        }

        Veiculo salvo = repository.save(veiculo);
        auditService.registrar("CONSULTAR_IA", "SUCESSO", "/veiculos/consultar",
                "id=" + salvo.getId(), request);
        return salvo.getEspecificacoesList();
    }
}
