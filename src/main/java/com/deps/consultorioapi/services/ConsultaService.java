package com.deps.consultorioapi.services;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.deps.consultorioapi.model.Consulta;
import com.deps.consultorioapi.model.Medico;
import com.deps.consultorioapi.model.Paciente;
import com.deps.consultorioapi.model.DTO.ConsultaDTO;
import com.deps.consultorioapi.repositories.ConsultaRepository;
import com.deps.consultorioapi.services.excecoes.ObjetoNaoEncontradoException;


@Service
public class ConsultaService {

    @Autowired
    ConsultaRepository consultaRepository;

    @Autowired
    MedicoService medicoService;

    @Autowired
    PacienteService pacienteService;

    public List<Consulta> listarConsultas(){
        return consultaRepository.findAll();
    }

    public Consulta buscarConsultaPorId(Long id){
    	Optional<Consulta> obj = consultaRepository.findById(id);
		return obj.orElseThrow(() -> new ObjetoNaoEncontradoException(
				"Objeto não encontrado! Id: " + id + ", Tipo: " + Paciente.class.getName()));
    } 
    
    public Consulta criarConsulta(ConsultaDTO consultaDTO) throws Exception {

        Medico medico = medicoService.buscarMedicoPorId(consultaDTO.getMedicoId());
        Paciente paciente = pacienteService.buscarPorId(consultaDTO.getPacienteId());

        if (consultaDTO.getDataConsulta().getDayOfWeek().equals(DayOfWeek.SATURDAY) || consultaDTO.getDataConsulta().getDayOfWeek().equals(DayOfWeek.SUNDAY)){
            throw new Exception("AGENDAMENTOS APENAS DE SEGUNDA À SEXTA");
        }

        int consultas = 0;

        for (Consulta consulta : medico.getConsultas()){
            if (consulta.getDataConsulta().isEqual(consultaDTO.getDataConsulta())){
                consultas++;
            }
        }

        if (consultas >= 10){
            throw new Exception("NÃO HÁ MAIS VAGAS PARA ESTE MÉDICO NESTA DATA");
        }

        if (medico != null && paciente != null){
            Consulta consulta = new Consulta(medico, paciente, consultaDTO.getDataConsulta());
            medico.addConsulta(consulta);
            paciente.addConsulta(consulta);

//            medicoService.atualizarMedico(medico.getId(), medico);
//            pacienteService.alterar(paciente);

            return consultaRepository.save(consulta);
        }
        else {
            throw new Exception("Medico ou paciente inválidos");
        }
    }

    public void apagarConsulta(Long id) throws Exception {

        Consulta consulta = consultaRepository.findById(id).get();
        if (consulta.getDataConsulta().isAfter(LocalDate.now())){

            consulta.getMedico().removerConsulta(consulta);
            consulta.getPaciente().removerConsulta(consulta);

            consultaRepository.save(consulta);
            consultaRepository.delete(consulta);
        }else{
            throw new Exception("Não é possível cancelar uma consulta passada!");
        }
    }

    public Consulta alterarConsulta(ConsultaDTO consultaDTO){

        Consulta consulta = this.buscarConsultaPorId(consultaDTO.getIdConsulta());
        Medico medico = medicoService.buscarMedicoPorId(consultaDTO.getMedicoId());
        Paciente paciente = pacienteService.buscarPorId(consultaDTO.getPacienteId());
        LocalDate dataConsulta = consultaDTO.getDataConsulta();

        consulta.setMedico(medico);
        consulta.setPaciente(paciente);
        consulta.setDataConsulta(dataConsulta);

        return consultaRepository.save(consulta);
    }


}
