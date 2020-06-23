package com.raminelli.pontointeligente.api.controllers;

import java.security.NoSuchAlgorithmException;

import javax.validation.Valid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.raminelli.pontointeligente.api.dtos.CadastroPjDto;
import com.raminelli.pontointeligente.api.entities.Empresa;
import com.raminelli.pontointeligente.api.entities.Funcionario;
import com.raminelli.pontointeligente.api.enums.PerfilEnum;
import com.raminelli.pontointeligente.api.response.Response;
import com.raminelli.pontointeligente.api.services.EmpresaService;
import com.raminelli.pontointeligente.api.services.FuncionarioService;
import com.raminelli.pontointeligente.api.utils.PasswordUtils;

@RestController
@RequestMapping("/api/cadastrar-pj")
@CrossOrigin(origins = "*")
public class CadastroPjController {

	private static final Logger log = LoggerFactory.getLogger(CadastroPjController.class);

	@Autowired
	private FuncionarioService funcionarioService;

	@Autowired
	private EmpresaService empresaService;

	public CadastroPjController() {
	}

	/**
	 * Cadastra uma pessoa jurídica no sistema.
	 * 
	 * @param CadastroPjDto
	 * @param result
	 * @return ResponseEntity<Response<CadastroPjDto>>
	 * @throws NoSuchAlgorithmException
	 */
	@PostMapping
	public ResponseEntity<Response<CadastroPjDto>> cadastrar(@Valid @RequestBody CadastroPjDto CadastroPjDto,
			BindingResult result) throws NoSuchAlgorithmException {
		log.info("Cadastrando PJ: {}", CadastroPjDto.toString());
		Response<CadastroPjDto> response = new Response<CadastroPjDto>();

		validarDadosExistentes(CadastroPjDto, result);
		Empresa empresa = this.converterDtoParaEmpresa(CadastroPjDto);
		Funcionario funcionario = this.converterDtoParaFuncionario(CadastroPjDto, result);

		if (result.hasErrors()) {
			log.error("Erro validando dados de cadastro PJ: {}", result.getAllErrors());
			result.getAllErrors().forEach(error -> response.getErrors().add(error.getDefaultMessage()));
			return ResponseEntity.badRequest().body(response);
		}

		this.empresaService.persistir(empresa);
		funcionario.setEmpresa(empresa);
		this.funcionarioService.persistir(funcionario);

		response.setData(this.converterCadastroPjDto(funcionario));
		return ResponseEntity.ok(response);
	}

	/**
	 * Verifica se a empresa ou funcionário já existem na base de dados.
	 * 
	 * @param CadastroPjDto
	 * @param result
	 */
	private void validarDadosExistentes(CadastroPjDto CadastroPjDto, BindingResult result) {
		this.empresaService.buscarPorCnpj(CadastroPjDto.getCnpj())
				.ifPresent(emp -> result.addError(new ObjectError("empresa", "Empresa já existente.")));

		this.funcionarioService.buscarPorCpf(CadastroPjDto.getCpf())
				.ifPresent(func -> result.addError(new ObjectError("funcionario", "CPF já existente.")));

		this.funcionarioService.buscarPorEmail(CadastroPjDto.getEmail())
				.ifPresent(func -> result.addError(new ObjectError("funcionario", "Email já existente.")));
	}

	/**
	 * Converte os dados do DTO para empresa.
	 * 
	 * @param CadastroPjDto
	 * @return Empresa
	 */
	private Empresa converterDtoParaEmpresa(CadastroPjDto CadastroPjDto) {
		Empresa empresa = new Empresa();
		empresa.setCnpj(CadastroPjDto.getCnpj());
		empresa.setRazaoSocial(CadastroPjDto.getRazaoSocial());

		return empresa;
	}

	/**
	 * Converte os dados do DTO para funcionário.
	 * 
	 * @param CadastroPjDto
	 * @param result
	 * @return Funcionario
	 * @throws NoSuchAlgorithmException
	 */
	private Funcionario converterDtoParaFuncionario(CadastroPjDto CadastroPjDto, BindingResult result)
			throws NoSuchAlgorithmException {
		Funcionario funcionario = new Funcionario();
		funcionario.setNome(CadastroPjDto.getNome());
		funcionario.setEmail(CadastroPjDto.getEmail());
		funcionario.setCpf(CadastroPjDto.getCpf());
		funcionario.setPerfil(PerfilEnum.ROLE_ADMIN);
		funcionario.setSenha(PasswordUtils.gerarBCrypt(CadastroPjDto.getSenha()));

		return funcionario;
	}

	/**
	 * Popula o DTO de cadastro com os dados do funcionário e empresa.
	 * 
	 * @param funcionario
	 * @return CadastroPjDto
	 */
	private CadastroPjDto converterCadastroPjDto(Funcionario funcionario) {
		CadastroPjDto CadastroPjDto = new CadastroPjDto();
		CadastroPjDto.setId(funcionario.getId());
		CadastroPjDto.setNome(funcionario.getNome());
		CadastroPjDto.setEmail(funcionario.getEmail());
		CadastroPjDto.setCpf(funcionario.getCpf());
		CadastroPjDto.setRazaoSocial(funcionario.getEmpresa().getRazaoSocial());
		CadastroPjDto.setCnpj(funcionario.getEmpresa().getCnpj());

		return CadastroPjDto;
	}

}