package br.com.challenge2026.challengeFord.controller;

import br.com.challenge2026.challengeFord.dto.ConsultaVeiculoDTO;
import br.com.challenge2026.challengeFord.dto.VeiculoDTO;
import br.com.challenge2026.challengeFord.model.Especificacoes;
import br.com.challenge2026.challengeFord.model.Veiculo;
import br.com.challenge2026.challengeFord.service.VeiculoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/veiculos")
@Validated
@Tag(name = "Veículos", description = "Endpoints para gerenciamento de veículos")
@SecurityRequirement(name = "bearerAuth")
public class VeiculoController {

    private static final String SAFE_TEXT = "^[A-Za-z0-9À-ÿ][A-Za-z0-9À-ÿ\\s.\\-_/]{0,79}$";

    @Autowired
    VeiculoService service;

    @Operation(summary = "Listar veículos", description = "Retorna todos os veículos cadastrados")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista retornada com sucesso"),
            @ApiResponse(responseCode = "401", description = "Não autenticado"),
            @ApiResponse(responseCode = "403", description = "Sem permissão")
    })
    @PreAuthorize("hasAnyRole('ADMIN','ANALISTA','USER')")
    @GetMapping
    public ResponseEntity<Page<VeiculoDTO>> listarVeiculos(
            @PageableDefault(size = 10, sort = "marca") Pageable pageable,
            HttpServletRequest request) {
        return ResponseEntity.ok(service.listarVeiculos(pageable, request));
    }

    @Operation(summary = "Buscar Veículo", description = "Retorna um veículo específico cadastrado")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Carro retornado com sucesso"),
            @ApiResponse(responseCode = "404", description = "Nenhum veículo encontrado")
    })
    @PreAuthorize("hasAnyRole('ADMIN','ANALISTA','USER')")
    @GetMapping("/buscar")
    public ResponseEntity<VeiculoDTO> buscarVeiculo(
            @RequestParam @NotBlank @Size(max = 80) @Pattern(regexp = SAFE_TEXT) String marca,
            @RequestParam @NotBlank @Size(max = 80) @Pattern(regexp = SAFE_TEXT) String modelo,
            @RequestParam @NotBlank @Size(max = 80) @Pattern(regexp = SAFE_TEXT) String versao) {
        return ResponseEntity.ok(service.buscarVeiculo(marca, modelo, versao));
    }

    @Operation(summary = "Salvar veículo", description = "Salva um veículo no banco")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Veículo salvo com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos")
    })
    @PreAuthorize("hasAnyRole('ADMIN','ANALISTA')")
    @PostMapping
    public ResponseEntity<VeiculoDTO> salvarVeiculo(@RequestBody @Valid Veiculo veiculo,
                                                    HttpServletRequest request) {
        return ResponseEntity.status(201).body(service.salvarVeiculo(veiculo, request));
    }

    @Operation(summary = "Consultar com IA", description = "Pesquise as especificações do veículo com IA")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Consulta realizada com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos")
    })
    @PreAuthorize("hasAnyRole('ADMIN','ANALISTA')")
    @PostMapping("/consultar")
    public ResponseEntity<List<Especificacoes>> consultarGemini(
            @RequestBody @Valid ConsultaVeiculoDTO dto,
            HttpServletRequest request) {
        return ResponseEntity.ok(service.consultarVeiculo(
                dto.marca(),
                dto.modelo(),
                dto.versao(),
                dto.prompt(),
                request
        ));
    }
}
