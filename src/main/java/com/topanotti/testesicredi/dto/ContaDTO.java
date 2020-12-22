package com.topanotti.testesicredi.dto;



import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ContaDTO {
    private Integer agencia;
    private String conta;
    private Double saldo;
    private String status;
}
