package com.example.trainticketoffice.service;

import com.example.trainticketoffice.model.Carriage;
import java.util.List;
import java.util.Optional;

public interface CarriageService {
    List<Carriage> getAllCarriages();
    Optional<Carriage> getCarriageById(Long id);
    Carriage saveCarriage(Carriage carriage);
    void deleteCarriage(Long id);
}