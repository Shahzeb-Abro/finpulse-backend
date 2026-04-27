package com.finpulse.service;

import com.finpulse.constants.Constants;
import com.finpulse.dto.request.AddWithdrawMoneyPotRequest;
import com.finpulse.dto.request.PotRequest;
import com.finpulse.dto.request.SearchDto;
import com.finpulse.dto.response.PotResponse;
import com.finpulse.entity.Lookup;
import com.finpulse.enums.LookupTypeEnum;
import com.finpulse.repository.LookupRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class AssistantPotTools {
    private final PotService potService;
    private final LookupRepository lookupRepository;

    @Tool(
            name = "GetAvailablePotThemes",
            description = "Returns all available pot themes with their names and IDs. "
                    + "Call this BEFORE creating or editing a pot so you can map "
                    + "the user's theme choice (e.g. 'red', 'blue') to a valid theme ID."
    )
    public String getAvailablePotThemes() {
        List<Lookup> themes = lookupRepository.findByLookupType(LookupTypeEnum.THEME.name());

        if (themes.isEmpty()) return "No themes available.";

        return themes.stream()
                .map(t -> "ID:%d | %s".formatted(t.getId(), t.getVisibleValue()))
                .collect(Collectors.joining("\n"));
    }

    @Tool(name = "GetAllPots",
            description = "Get all savings pots for the current user with full details. "
                    + "Call this BEFORE any pot action (edit, delete, add money, withdraw) "
                    + "so you can resolve the pot the user is referring to by name and get current values.")
    public String getAllPots() {
        try {
            SearchDto dto = new SearchDto();
            dto.setPage(0);
            dto.setPageSize(200);
            dto.setSort("createdDate,desc");

            List<PotResponse> pots = potService.getAllPots(dto).getBody().getData().getContent();

            if (pots.isEmpty()) return "No pots found.";

            return pots.stream()
                    .map(p -> "ID:%d | %s | Theme: %s (themeId:%d) | Saved: %s / Target: %s"
                            .formatted(p.getId(), p.getName(),
                                    p.getThemeId(), p.getThemeId(),
                                    p.getTotalSaved(), p.getTargetAmount()))
                    .collect(Collectors.joining("\n"));
        } catch (Exception e) {
            e.printStackTrace();
            return "Error fetching pots.";
        }
    }

    @Tool(
            name = "CreatePot",
            description = "Create a new savings pot. "
                    + "Requires: name (string), targetAmount (number > 0), themeId (long). "
                    + "You MUST call GetAvailablePotThemes first to resolve the theme name "
                    + "the user mentioned into a valid themeId. "
                    + "Example flow: user says 'create a trip pot with red theme and 5000 target' "
                    + "→ call GetAvailablePotThemes → find 'Red' = ID 7 "
                    + "→ call CreatePot(name='Trip', targetAmount=5000, themeId=7)"
    )
    public String createPot(@ToolParam(description = "Name of the pot, must be less or equal to 30 characters") String name,
                            @ToolParam(description = "Amount we are targeting to save in this pot") double targetAmount,
                            @ToolParam(description = "Theme ID associated with every color in the lookups, you MUST call GetAvailablePotThemes to get ID associated with color") long themeId) {
        try {
            PotRequest request = new PotRequest();
            request.setName(name);
            request.setTargetAmount(BigDecimal.valueOf(targetAmount));
            request.setThemeId(themeId);

            var response = potService.createPot(request);
            var body = response.getBody();

            if (response.getStatusCode().is2xxSuccessful()) {
                var pot = body.getData();
                return "Pot created! ID:%d | %s | Target: %s"
                        .formatted(pot.getId(), pot.getName(), pot.getTargetAmount());
            }
            return "Failed: " + body.getMessage();
        } catch (Exception e) {
            return "Error creating pot: " + e.getMessage();
        }
    }

    @Tool(
            name = "EditPot",
            description = "Edit an existing savings pot. Requires: potId, name, targetAmount, themeId. "
                    + "Call GetAllPots first to get the pot's CURRENT values (name, targetAmount, themeId). "
                    + "Call GetAvailablePotThemes ONLY if the user wants to change the theme. "
                    + "CRITICAL: For any field the user did NOT ask to change, you MUST pass "
                    + "the EXACT current value from GetAllPots. Do NOT guess or change other fields. "
                    + "Example: user says 'rename my Trip pot to Vacation' "
                    + "→ GetAllPots shows ID:3 | Trip | Theme: Red (themeId:7) | Target: 5000 "
                    + "→ call EditPot(potId=3, name='Vacation', targetAmount=5000, themeId=7) "
                    + "keeping targetAmount and themeId exactly as they were."
    )
    public String editPot(
            @ToolParam(description = "ID of the pot to edit, get this from GetAllPots") long potId,
            @ToolParam(description = "New name for the pot, must be <= 30 characters") String name,
            @ToolParam(description = "New target amount for the pot") double targetAmount,
            @ToolParam(description = "Theme ID, resolve via GetAvailablePotThemes") long themeId) {
        try {
            PotRequest request = new PotRequest();
            request.setName(name);
            request.setTargetAmount(BigDecimal.valueOf(targetAmount));
            request.setThemeId(themeId);

            var response = potService.editPot(potId, request);
            var body = response.getBody();

            if (response.getStatusCode().is2xxSuccessful()) {
                var pot = body.getData();
                return "Pot updated! ID:%d | %s | Target: %s"
                        .formatted(pot.getId(), pot.getName(), pot.getTargetAmount());
            }
            return "Failed: " + body.getMessage();
        } catch (Exception e) {
            return "Error editing pot: " + e.getMessage();
        }
    }

    @Tool(
            name = "DeletePot",
            description = "Delete (deactivate) a savings pot. "
                    + "Call GetAllPots first to resolve the pot the user is referring to by name into a potId. "
                    + "Always confirm with the user before deleting. "
                    + "Show the user the pot's current details (name, saved amount, target) before asking for confirmation "
                    + "so they understand what they are deleting. "
                    + "Example: user says 'delete my trip pot' "
                    + "→ GetAllPots shows ID:3 | Trip | Saved: 2000 / Target: 5000 "
                    + "→ ask user to confirm deletion of 'Trip' which has 2000 saved "
                    + "→ user says 'yes' → call DeletePot(potId=3)"
    )
    public String deletePot(
            @ToolParam(description = "ID of the pot to delete, get this from GetAllPots") long potId) {
        try {
            var response = potService.deletePotById(potId);

            if (response.getStatusCode().is2xxSuccessful()) {
                return "Pot deleted successfully.";
            }
            return "Failed: " + response.getBody().getMessage();
        } catch (Exception e) {
            return "Error deleting pot: " + e.getMessage();
        }
    }

    @Tool(
            name = "AddMoneyToPot",
            description = "Add money to an existing savings pot. "
                    + "Call GetAllPots first to resolve the pot name to a potId and to check current saved/target amounts. "
                    + "The amount cannot exceed the remaining target (targetAmount - totalSaved). "
                    + "IMPORTANT: Use the 'Remaining' value from GetAllPots to verify the amount fits before calling this. "
                    + "If the user's amount exceeds the remaining, tell them the maximum they can add instead of failing. "
                    + "Example: user says 'add 500 to my trip pot' "
                    + "→ GetAllPots shows ID:3 | Trip | Saved: 2000 / Target: 5000 | Remaining: 3000 "
                    + "→ 500 <= 3000, so proceed "
                    + "→ call AddMoneyToPot(potId=3, amount=500)"
    )
    public String addMoneyToPot(
            @ToolParam(description = "ID of the pot to add money to, get this from GetAllPots") long potId,
            @ToolParam(description = "Amount to add, must be > 0 and cannot exceed remaining target") double amount) {
        try {
            var request = new AddWithdrawMoneyPotRequest();
            request.setAmount(BigDecimal.valueOf(amount));

            var response = potService.addMoneyToPot(potId, request);
            var body = response.getBody();

            if (response.getStatusCode().is2xxSuccessful()) {
                var pot = body.getData();
                return "Added! %s now has %s / %s saved."
                        .formatted(pot.getName(), pot.getTotalSaved(), pot.getTargetAmount());
            }
            return "Failed: " + body.getMessage();
        } catch (Exception e) {
            return "Error adding money: " + e.getMessage();
        }
    }

    @Tool(
            name = "WithdrawMoneyFromPot",
            description = "Withdraw money from an existing savings pot. "
                    + "Call GetAllPots first to resolve the pot name to a potId and to check the current saved amount. "
                    + "The amount cannot exceed the total saved in the pot. "
                    + "IMPORTANT: Use the 'Saved' value from GetAllPots to verify the amount is available before calling this. "
                    + "If the user's amount exceeds what's saved, tell them the maximum they can withdraw instead of failing. "
                    + "Example: user says 'withdraw 1000 from savings' "
                    + "→ GetAllPots shows ID:1 | Savings | Saved: 400 / Target: 30000 "
                    + "→ 1000 > 400, so tell user: 'You only have 400 saved in Savings. Would you like to withdraw that instead?' "
                    + "→ if user says withdraw 200 → call WithdrawMoneyFromPot(potId=1, amount=200)"
    )
    public String withdrawMoneyFromPot(
            @ToolParam(description = "ID of the pot to withdraw from, get this from GetAllPots") long potId,
            @ToolParam(description = "Amount to withdraw, must be > 0 and cannot exceed total saved") double amount) {
        try {
            var request = new AddWithdrawMoneyPotRequest();
            request.setAmount(BigDecimal.valueOf(amount));

            var response = potService.withdrawMoneyFromPot(potId, request);
            var body = response.getBody();

            if (response.getStatusCode().is2xxSuccessful()) {
                var pot = body.getData();
                return "Withdrawn! %s now has %s / %s saved."
                        .formatted(pot.getName(), pot.getTotalSaved(), pot.getTargetAmount());
            }
            return "Failed: " + body.getMessage();
        } catch (Exception e) {
            return "Error withdrawing money: " + e.getMessage();
        }
    }

}
