package bakery.service;

import bakery.dto.request.IngredientDTO;
import bakery.entity.Ingredient;
import org.springframework.data.domain.Page;

import java.math.BigDecimal;
import java.util.List;

public interface IngredientService {

    List<Ingredient> findAll();
    Ingredient findById(Long id);
    void importNewIngredient(String name, String unit, BigDecimal minQuantity);
    void updateIngredient(Ingredient ingredient);  // cho edit
    void deleteById(Long id);
    void exportIngredient(Long ingredientId, BigDecimal amount) throws Exception;
    void importStock(List<IngredientDTO> importList);
    void exportStock(List<IngredientDTO> exportList);
    Page<Ingredient> filterAndPaginate(String keyword, String status, int page, int size);
}
