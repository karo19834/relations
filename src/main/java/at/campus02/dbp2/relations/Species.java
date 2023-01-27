package at.campus02.dbp2.relations;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
public class Species {

    @Id @GeneratedValue
    private Integer id;

    private String name;

    @OneToMany (mappedBy = "species", cascade = CascadeType.ALL, orphanRemoval = true )
    private final List<Animal> animals = new ArrayList<>();

    public Species(String name) {
        this.name = name;
    }

    public Species() {
    }

    public Integer getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<Animal> getAnimals() {
        return animals;
    }

}
