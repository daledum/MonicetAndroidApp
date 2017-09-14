package net.monicet.monicet;

import java.io.Serializable;

/**
 * Created by ubuntu on 23-01-2017.
 */

public class Animal implements Serializable {

    private int startQuantity;
    private int endQuantity;
    private String age;

    private final Specie specie;

    public Animal(Specie vSpecie) {
        specie = vSpecie;
        startQuantity = 0;
        endQuantity = 0;//Utils.INITIAL_VALUE; was -1, or 99 so that I can use it in the Number Picker?
        age = "";
    }

    public Animal(Animal vAnimal) {
        this(vAnimal.getSpecie());
        startQuantity = vAnimal.getStartQuantity();
        endQuantity = vAnimal.getEndQuantity();
        age = vAnimal.getAge();
    }

    public Specie getSpecie() { return specie; }

    public int getStartQuantity() { return startQuantity; }
    public void setStartQuantity(int vQuantity) { startQuantity = vQuantity; }

    public int getEndQuantity() { return endQuantity; }
    public void setEndQuantity(int vEndQuantity) { endQuantity = vEndQuantity; }

    public String getAge() { return age; }
    public void setAge(String vAge) { age = vAge; }

}
