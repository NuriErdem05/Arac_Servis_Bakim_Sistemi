package com.nurierdem.vehicle_service.entity;

import com.nurierdem.vehicle_service.enums.FuelType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "vehicles")
public class Vehicle {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    //Her araç bir kullanıcıya aittir.
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @Column(nullable = false, unique = true, length = 20)
    private String plate;

    @Column(nullable = false, length = 100)
    private String brand;

    @Column(nullable = false, length = 100)
    private String model;

    @Column(name = "model_year", nullable = false)
    private Integer modelYear;

    //Yakıt türünü GASOLINE, DIESEL gibi metin olarak saklar.
    @Enumerated(EnumType.STRING)
    @Column(name = "fuel_type", nullable = false, length = 20)
    private FuelType fuelType;

    @Column(name = "engine_info", length = 150)
    private String engineInfo;

    @Column(name = "current_mileage", nullable = false)
    private Integer currentMileage;

    //Geçmişi koruyarak aracı arşivlemek için kullanılacak.
    @Column(nullable = false)
    private boolean archived = false;

    public Vehicle(){}
}
