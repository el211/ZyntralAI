package com.zyntral.modules.crm.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "crm_prospects")
public class CrmProspect {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "workspace_id", nullable = false)
    private UUID workspaceId;

    @Column(name = "added_by", nullable = false)
    private UUID addedBy;

    @Column(name = "first_name", nullable = false)
    private String firstName;

    @Column(name = "last_name")
    private String lastName;

    @Column(name = "company")
    private String company;

    @Column(name = "website")
    private String website;

    @Column(name = "email")
    private String email;

    @Column(name = "phone")
    private String phone;

    @Column(name = "linkedin_url")
    private String linkedinUrl;

    @Column(name = "country")
    private String country;

    @Column(name = "industry")
    private String industry;

    @Column(name = "status", nullable = false)
    private String status = "NEW";

    @Column(name = "notes", columnDefinition = "text")
    private String notes;

    @Column(name = "last_contacted_at")
    private Instant lastContactedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    protected CrmProspect() {}

    public static CrmProspect create(UUID workspaceId, UUID addedBy,
                                      String firstName, String lastName, String company,
                                      String website, String email, String phone,
                                      String linkedinUrl, String country, String industry, String notes) {
        CrmProspect p = new CrmProspect();
        p.workspaceId = workspaceId;
        p.addedBy = addedBy;
        p.firstName = firstName;
        p.lastName = lastName;
        p.company = company;
        p.website = website;
        p.email = email;
        p.phone = phone;
        p.linkedinUrl = linkedinUrl;
        p.country = country;
        p.industry = industry;
        p.notes = notes;
        return p;
    }

    public void update(String firstName, String lastName, String company,
                       String website, String email, String phone,
                       String linkedinUrl, String country, String industry,
                       String notes, String status) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.company = company;
        this.website = website;
        this.email = email;
        this.phone = phone;
        this.linkedinUrl = linkedinUrl;
        this.country = country;
        this.industry = industry;
        this.notes = notes;
        this.status = status;
        this.updatedAt = Instant.now();
    }

    public void markContacted() {
        this.lastContactedAt = Instant.now();
        if ("NEW".equals(this.status)) this.status = "CONTACTED";
        this.updatedAt = Instant.now();
    }

    public UUID getId()                   { return id; }
    public UUID getWorkspaceId()          { return workspaceId; }
    public UUID getAddedBy()              { return addedBy; }
    public String getFirstName()          { return firstName; }
    public String getLastName()           { return lastName; }
    public String getCompany()            { return company; }
    public String getWebsite()            { return website; }
    public String getEmail()              { return email; }
    public String getPhone()              { return phone; }
    public String getLinkedinUrl()        { return linkedinUrl; }
    public String getCountry()            { return country; }
    public String getIndustry()           { return industry; }
    public String getStatus()             { return status; }
    public String getNotes()              { return notes; }
    public Instant getLastContactedAt()   { return lastContactedAt; }
    public Instant getCreatedAt()         { return createdAt; }
    public Instant getUpdatedAt()         { return updatedAt; }
}
