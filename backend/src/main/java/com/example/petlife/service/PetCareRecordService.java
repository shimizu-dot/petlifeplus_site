package com.example.petlife.service;

import com.example.petlife.config.LoginUser;
import com.example.petlife.dto.pet.PetCareRecordForm;
import com.example.petlife.entity.PetCareRecordEntity;
import com.example.petlife.entity.PetEntity;
import com.example.petlife.mapper.PetCareRecordMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class PetCareRecordService {

    private final PetCareRecordMapper petCareRecordMapper;
    private final PetService petService;

    public PetCareRecordService(PetCareRecordMapper petCareRecordMapper, PetService petService) {
        this.petCareRecordMapper = petCareRecordMapper;
        this.petService = petService;
    }

    public void addRecord(Long petId, PetCareRecordForm form, LoginUser currentUser) {
        PetEntity pet = petService.getEntity(petId, currentUser);
        petService.ensurePetUsable(pet);

        LocalDate administeredOn = form.getAdministeredOn() != null ? form.getAdministeredOn() : LocalDate.now();
        LocalDate nextDueOn = form.getNextDueOn() != null ? form.getNextDueOn() : administeredOn.plusYears(1);

        PetCareRecordEntity row = new PetCareRecordEntity(
                null,
                pet.id(),
                currentUser.id(),
                form.getCareType(),
                administeredOn,
                nextDueOn,
                form.getMemo(),
                null,
                null,
                null
        );
        petCareRecordMapper.insertReturningId(row);
    }

    public List<PetCareRecordEntity> listByPet(Long petId, LoginUser currentUser) {
        petService.getEntity(petId, currentUser);
        return petCareRecordMapper.findByPetId(petId);
    }

    public List<PetCareRecordEntity> listUpcomingNotices(Long petId, LoginUser currentUser) {
        petService.getEntity(petId, currentUser);
        LocalDate from = LocalDate.now();
        LocalDate to = from.plusMonths(2);
        return petCareRecordMapper.findUpcomingByPetId(petId, from, to);
    }
}
