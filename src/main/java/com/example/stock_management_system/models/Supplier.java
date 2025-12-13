package com.example.stock_management_system.models;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class Supplier {

    private final IntegerProperty id;
    private final StringProperty name;
    private final StringProperty contact;

    public Supplier(int id, String name, String contact) {
        this.id = new SimpleIntegerProperty(id);
        this.name = new SimpleStringProperty(name);
        this.contact = new SimpleStringProperty(contact);
    }

    public IntegerProperty idProperty() {
        return id;
    }

    public StringProperty nameProperty() {
        return name;
    }

    public StringProperty contactProperty() {
        return contact;
    }

    public int getId() {
        return id.get();
    }

    public String getName() {
        return name.get();
    }

    public String getContact() {
        return contact.get();
    }

    public void setName(String name) {
        this.name.set(name);
    }

    public void setContact(String contact) {
        this.contact.set(contact);
    }
}
