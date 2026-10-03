package com.travellens.backend.model;

import jakarta.persistence.*;

@Entity
@Table(name = "destination")
public class Destination {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    private String category; // Beach, Hill Station, Nature, Heritage, Adventure...

    @Column(length = 2000)
    private String description;

    private String bestTimeToVisit;

    private String budgetEstimate;

    private String recommendedDuration;

    @Column(length = 2000)
    private String mustVisitAttractions;

    @Column(length = 2000)
    private String tips;

    @Column(length = 2000)
    private String thingsToAvoid;

    private String imageUrl;

    public Destination() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getBestTimeToVisit() {
        return bestTimeToVisit;
    }

    public void setBestTimeToVisit(String bestTimeToVisit) {
        this.bestTimeToVisit = bestTimeToVisit;
    }

    public String getBudgetEstimate() {
        return budgetEstimate;
    }

    public void setBudgetEstimate(String budgetEstimate) {
        this.budgetEstimate = budgetEstimate;
    }

    public String getRecommendedDuration() {
        return recommendedDuration;
    }

    public void setRecommendedDuration(String recommendedDuration) {
        this.recommendedDuration = recommendedDuration;
    }

    public String getMustVisitAttractions() {
        return mustVisitAttractions;
    }

    public void setMustVisitAttractions(String mustVisitAttractions) {
        this.mustVisitAttractions = mustVisitAttractions;
    }

    public String getTips() {
        return tips;
    }

    public void setTips(String tips) {
        this.tips = tips;
    }

    public String getThingsToAvoid() {
        return thingsToAvoid;
    }

    public void setThingsToAvoid(String thingsToAvoid) {
        this.thingsToAvoid = thingsToAvoid;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }
}
