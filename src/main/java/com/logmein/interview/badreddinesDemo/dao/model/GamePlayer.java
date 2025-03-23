package com.logmein.interview.badreddinesDemo.dao.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@Entity
public class GamePlayer {
    @EqualsAndHashCode.Include
    @Id
    private GamePlayerPk id;

    @EqualsAndHashCode.Exclude
    private Integer playerOrder;
    @EqualsAndHashCode.Exclude
    @ManyToOne
    private Game game;
    @EqualsAndHashCode.Exclude
    @OneToOne
    private Player player;


}
