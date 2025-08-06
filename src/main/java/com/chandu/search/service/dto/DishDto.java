package com.chandu.search.service.dto;


import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class DishDto {
    private String name;
    private String category;
}
