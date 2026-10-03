package com.travellens.backend.config;

import com.travellens.backend.model.Advisor;
import com.travellens.backend.model.AdvisorAvailability;
import com.travellens.backend.model.Destination;
import com.travellens.backend.repository.AdvisorAvailabilityRepository;
import com.travellens.backend.repository.AdvisorRepository;
import com.travellens.backend.repository.DestinationRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Seeds a handful of demo destinations, advisors and open consultation
 * slots on first run so the app is not empty when you demo it.
 * Runs only if the destination table is empty.
 */
@Component
public class DataSeeder implements CommandLineRunner {

    private final DestinationRepository destinationRepository;
    private final AdvisorRepository advisorRepository;
    private final AdvisorAvailabilityRepository availabilityRepository;

    public DataSeeder(DestinationRepository destinationRepository,
                       AdvisorRepository advisorRepository,
                       AdvisorAvailabilityRepository availabilityRepository) {
        this.destinationRepository = destinationRepository;
        this.advisorRepository = advisorRepository;
        this.availabilityRepository = availabilityRepository;
    }

    @Override
    public void run(String... args) {
        if (destinationRepository.count() > 0) {
            return;
        }

        Destination goa = buildDestination("Goa", "Beach",
                "Golden beaches, Portuguese-era churches, and a laid-back coastal vibe on India's west coast.",
                "November to February", "Rs. 8,000 - 20,000 for 4 days", "3-5 days",
                "Baga Beach, Fort Aguada, Basilica of Bom Jesus, Dudhsagar Falls",
                "Rent a scooter to explore beyond the crowded beaches; book stays early in peak season.",
                "Avoid the beaches during monsoon season (June-Sept) due to strong currents.");

        Destination manali = buildDestination("Manali", "Hill Station",
                "A snow-capped Himalayan town popular for adventure sports, scenic valleys, and cool weather.",
                "March to June, October to February for snow", "Rs. 10,000 - 25,000 for 5 days", "4-6 days",
                "Solang Valley, Rohtang Pass, Hadimba Temple, Old Manali",
                "Carry warm clothes even in summer; book Rohtang Pass permits in advance.",
                "Avoid visiting during heavy monsoon landslide season (July-Aug) on hill roads.");

        Destination jim = buildDestination("Jim Corbett National Park", "Nature",
                "India's oldest national park, known for tiger sightings and dense Himalayan foothill forests.",
                "November to June", "Rs. 6,000 - 15,000 for 3 days", "2-3 days",
                "Dhikala Zone Safari, Corbett Waterfall, Garjiya Devi Temple",
                "Book safari permits weeks in advance; early morning safaris have better sighting odds.",
                "Park remains closed during monsoon (mid-June to mid-November) for core zones.");

        Destination jaipur = buildDestination("Jaipur", "Heritage",
                "The Pink City, famous for its majestic forts, palaces, and vibrant bazaars.",
                "October to March", "Rs. 7,000 - 18,000 for 4 days", "3-4 days",
                "Amber Fort, City Palace, Hawa Mahal, Jantar Mantar",
                "Hire a licensed guide at forts; bargain at local bazaars for handicrafts.",
                "Avoid outdoor sightseeing during peak summer afternoons (April-June).");

        Destination rishikesh = buildDestination("Rishikesh", "Adventure",
                "The 'Yoga Capital of the World', also known for white-water rafting and cliff jumping on the Ganges.",
                "September to November, February to April", "Rs. 5,000 - 12,000 for 3 days", "2-4 days",
                "River Rafting, Laxman Jhula, Beatles Ashram, Neer Garh Waterfall",
                "Book rafting through certified operators only; carry a reusable water bottle.",
                "Avoid rafting during monsoon (July-Aug) when river currents are dangerous.");

        destinationRepository.save(goa);
        destinationRepository.save(manali);
        destinationRepository.save(jim);
        destinationRepository.save(jaipur);
        destinationRepository.save(rishikesh);

        Advisor advisor1 = buildAdvisor("Ananya Kapoor",
                "Local Goa-based travel consultant with 6 years of experience planning coastal getaways.", 6, goa);
        Advisor advisor2 = buildAdvisor("Rohit Thakur",
                "Manali native and trekking guide who has led over 200 Himalayan expeditions.", 9, manali);
        Advisor advisor3 = buildAdvisor("Meera Nair",
                "Wildlife enthusiast and certified Jim Corbett safari planner.", 5, jim);
        Advisor advisor4 = buildAdvisor("Vikram Singh",
                "Heritage walk expert specializing in Rajasthan's forts and palaces.", 8, jaipur);
        Advisor advisor5 = buildAdvisor("Priya Sharma",
                "Adventure sports coordinator based in Rishikesh, specializing in rafting trips.", 4, rishikesh);

        advisorRepository.save(advisor1);
        advisorRepository.save(advisor2);
        advisorRepository.save(advisor3);
        advisorRepository.save(advisor4);
        advisorRepository.save(advisor5);

        for (Advisor advisor : new Advisor[]{advisor1, advisor2, advisor3, advisor4, advisor5}) {
            for (int day = 1; day <= 3; day++) {
                availabilityRepository.save(buildSlot(advisor, LocalDate.now().plusDays(day), LocalTime.of(10, 0), LocalTime.of(10, 30)));
                availabilityRepository.save(buildSlot(advisor, LocalDate.now().plusDays(day), LocalTime.of(15, 0), LocalTime.of(15, 30)));
            }
        }
    }

    private Destination buildDestination(String name, String category, String description, String bestTime,
                                          String budget, String duration, String attractions, String tips,
                                          String avoid) {
        Destination d = new Destination();
        d.setName(name);
        d.setCategory(category);
        d.setDescription(description);
        d.setBestTimeToVisit(bestTime);
        d.setBudgetEstimate(budget);
        d.setRecommendedDuration(duration);
        d.setMustVisitAttractions(attractions);
        d.setTips(tips);
        d.setThingsToAvoid(avoid);
        return d;
    }

    private Advisor buildAdvisor(String name, String bio, int years, Destination destination) {
        Advisor a = new Advisor();
        a.setName(name);
        a.setBio(bio);
        a.setYearsOfExperience(years);
        a.setDestination(destination);
        return a;
    }

    private AdvisorAvailability buildSlot(Advisor advisor, LocalDate date, LocalTime start, LocalTime end) {
        AdvisorAvailability slot = new AdvisorAvailability();
        slot.setAdvisor(advisor);
        slot.setDate(date);
        slot.setStartTime(start);
        slot.setEndTime(end);
        slot.setBooked(false);
        return slot;
    }
}
