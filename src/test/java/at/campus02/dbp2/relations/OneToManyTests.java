package at.campus02.dbp2.relations;

import org.hamcrest.CoreMatchers;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;

public class OneToManyTests {

    private EntityManagerFactory factory;
    private EntityManager manager;

    @BeforeEach
    public void setup() {
        factory = Persistence
                .createEntityManagerFactory("persistenceUnitName");
        manager = factory.createEntityManager();

    }

    @AfterEach
    public void teardown() {
        if (manager.isOpen()) {
            manager.close();
        }
        if (factory.isOpen()) {
            factory.close();
        }
    }

    @Test
    public void persistSpeciesWithCascadeStoresAnimalsInDatabase() {
        // given
        Animal bunny = new Animal("Bunny");
        Animal dog = new Animal("dog");
        Species birds = new Species("birds");

        // Referenzen für FK in der DB
        bunny.setSpecies(birds);
        dog.setSpecies(birds);
        // Referenzen für CASCADE
        birds.getAnimals().add(bunny);
        birds.getAnimals().add(dog);

        // when
        manager.getTransaction().begin();
        manager.persist(birds);
        manager.getTransaction().commit();

        manager.clear();

        // then
        Species birdsFromDB = manager.find(Species.class, birds.getId());
        assertThat(birdsFromDB.getAnimals().size(), is(2));
        assertThat(birdsFromDB.getAnimals(), containsInAnyOrder(bunny, dog));
    }

    /**
     * <pre>{@code
     *     // -------------------------------------------
     *     // given
     *     Animal clownfish = new Animal("Nemo");
     *     Animal squirrel = new Animal("Squirrel");
     *     Species fish = new Species("Fish");
     *
     *     // Referenzen für DB
     *     clownfish.setSpecies(fish);
     *     // FEHLER -> den wollen wir mit einem Update korrigieren
     *     squirrel.setSpecies(fish);
     *
     *     // Referenzen für CASCADE
     *     fish.getAnimals().add(clownfish);
     *     fish.getAnimals().add(squirrel);
     * }</pre>
     *  <img src="https://raw.githubusercontent.com/mmair/dbp2/main/examples/03_relations/src/test/resources/update_example_values.png" />
     *  <pre>{@code
     *     // Speichern (mit Fehler)
     *     manager.getTransaction().begin();
     *     manager.persist(fish);
     *     manager.getTransaction().commit();
     *  }</pre>
     *  <img src="https://raw.githubusercontent.com/mmair/dbp2/main/examples/03_relations/src/test/resources/update_example_persist.png" />
     *  <pre>{@code
     *     // -------------------------------------------
     *     // when: Korrekturversuch, zum Scheitern verurteilt...
     *
     *     manager.getTransaction().begin();
     *     // löst die Referenz nur im Speicher, nicht in der Datenbank!
     *     fish.getAnimals().remove(squirrel);
     *     manager.merge(fish);
     *     manager.getTransaction().commit();
     *
     *     manager.clear();
     *  }</pre>
     *  <img src="https://raw.githubusercontent.com/mmair/dbp2/main/examples/03_relations/src/test/resources/update_example_wrong.png" />
     *  <pre>{@code
     *     // -------------------------------------------
     *     // when: Korrekturversuch, diesmal richtig...
     *
     *     manager.getTransaction().begin();
     *     fish.getAnimals().remove(squirrel);
     *     manager.merge(fish);
     *     // erst die folgende Zeile löscht die Relation tatsächlich!
     *     manager.merge(squirrel).setSpecies(null);
     *     manager.getTransaction().commit();
     *  }</pre>
     *  <img src="https://raw.githubusercontent.com/mmair/dbp2/main/examples/03_relations/src/test/resources/update_example_correct.png" />
     */
    @Test
    @Disabled("Only works without orphanRemoval - enable after setting orphanRemoval to false")
    public void updateExampleWithCorrectingReferences() {
        // -----------------------------------------------------
        // given
        Animal clownfish = new Animal("Nemo");
        Animal squirrel = new Animal("Squirrel");
        Species fish = new Species("Fish");

        // Referenzen für DB
        clownfish.setSpecies(fish);
        // FEHLER -> den wollen wir dann korrigieren
        squirrel.setSpecies(fish);

        // Referenzen fürs CASCADE
        fish.getAnimals().add(clownfish);
        fish.getAnimals().add(squirrel);

        // -> Bild dazu: update_example_values.png

        // Speichern
        manager.getTransaction().begin();
        manager.persist(fish);
        manager.getTransaction().commit();

        manager.clear();

        // -> Bild dazu: update_example_persist.png

        // -----------------------------------------------------
        // when: Korrekturversuch, zum Scheitern verurteilt...
        manager.getTransaction().begin();
        fish.getAnimals().remove(squirrel);
        manager.merge(fish);
        manager.getTransaction().commit();
        manager.clear();

        // -> Bild dazu: update_example_wrong.png

        // -----------------------------------------------------
        // then
        // Squirrel existiert noch in DB
        Animal squirrelFromDb = manager.find(Animal.class, squirrel.getId());
        assertThat(squirrelFromDb, is(notNullValue()));

        // Squirrel ist immer noch ein Fisch - wir haben im Speicher die Liste von
        // "Fish" geändert, aber species von Squirrel zeigt nach wie vor auf Fish,
        // auch in der DB.
        assertThat(squirrelFromDb.getSpecies().getId(), is(fish.getId()));

        // auch wenn wir die Liste mittels "refresh" neu einlesen, wird die
        // Referenz von Squirrel auf Fish (DB) neu eingelesen und Squirrel ist
        // wieder in der Liste drin.
        Species mergedFish = manager.merge(fish);
        manager.refresh(mergedFish);
        assertThat(mergedFish.getAnimals().size(), is(2));

        // -----------------------------------------------------
        // when: Korrekturversuch, diesmal richtig ...
        manager.getTransaction().begin();
        squirrel.setSpecies(null);
        manager.merge(squirrel);
        manager.getTransaction().commit();
        manager.clear();

        // -> Bild dazu: update_example_correct.png

        // -----------------------------------------------------
        // then
        // Squirrel existiert noch in DB
        squirrelFromDb = manager.find(Animal.class, squirrel.getId());
        assertThat(squirrelFromDb, is(notNullValue()));

        // Squirrel ist kein Fisch mehr
        assertThat(squirrelFromDb.getSpecies(), is(nullValue()));

        // auch wenn wir die Liste mittels "refresh" neu einlesen, ist Squirrel
        // nicht mehr enthalten
        mergedFish = manager.merge(fish);
        manager.refresh(mergedFish);
        assertThat(mergedFish.getAnimals().size(), is(1));
    }

    @Test
    public void orphanRemovalDeletesOrphansFromDatabase() {


        Animal clownfish = new Animal("Nemo");
        Animal squirrel = new Animal("Squirrel");
        Species fish = new Species("Fish");


        clownfish.setSpecies(fish);
        squirrel.setSpecies(fish);


        fish.getAnimals().add(clownfish);
        fish.getAnimals().add(squirrel);


        manager.getTransaction().begin();
        manager.persist(fish);
        manager.getTransaction().commit();

        manager.clear();


        manager.getTransaction().begin();
        fish.getAnimals().remove(squirrel);
        manager.merge(fish);
        manager.getTransaction().commit();

        manager.clear();


        Animal squirrelFromDb = manager.find(Animal.class, squirrel.getId());
        assertThat(squirrelFromDb, is(nullValue()));

        Species refreshedFish = manager.merge(fish);
        manager.refresh(refreshedFish);

        assertThat(refreshedFish.getAnimals().size(), is(1));
    }
}
