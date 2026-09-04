package com.scaler.productcatalog.service;

import com.scaler.productcatalog.dto.product.ImageCreateRequestDTO;
import com.scaler.productcatalog.dto.product.ImageResponseDTO;
import com.scaler.productcatalog.dto.product.ImageUpdateRequestDTO;
import com.scaler.productcatalog.dto.product.ProductCreateRequestDTO;
import com.scaler.productcatalog.dto.product.ProductResponseDTO;
import com.scaler.productcatalog.dto.product.ProductUpdateRequestDTO;
import com.scaler.productcatalog.enums.ProductStatus;
import com.scaler.productcatalog.exception.BadRequestException;
import com.scaler.productcatalog.exception.ConflictException;
import com.scaler.productcatalog.exception.NotFoundException;
import com.scaler.productcatalog.mapper.ProductMapper;
import com.scaler.productcatalog.model.Product;
import com.scaler.productcatalog.model.ProductCategory;
import com.scaler.productcatalog.model.ProductCategoryId;
import com.scaler.productcatalog.model.ProductImage;
import com.scaler.productcatalog.model.ProductInventory;
import com.scaler.productcatalog.repository.CategoryRepository;
import com.scaler.productcatalog.repository.ProductCategoryRepository;
import com.scaler.productcatalog.repository.ProductImageRepository;
import com.scaler.productcatalog.repository.ProductInventoryRepository;
import com.scaler.productcatalog.repository.ProductRepository;
import com.scaler.productcatalog.specification.ProductSpecifications;
import com.scaler.productcatalog.web.PageableUtils;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private static final String AGG_PRODUCT = "PRODUCT";

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final ProductCategoryRepository productCategoryRepository;
    private final ProductImageRepository productImageRepository;
    private final ProductInventoryRepository productInventoryRepository;
    private final OutboxWriter outboxWriter;

    // ---------- Commands ----------

    @Override
    @Transactional
    public ProductResponseDTO create(ProductCreateRequestDTO request) {
        if (productRepository.existsBySku(request.getSku())) {
            throw new ConflictException("DUPLICATE_SKU", "Product SKU already exists: " + request.getSku());
        }
        Product product = new Product();
        product.setSku(request.getSku());
        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setBrand(request.getBrand());
        product.setBasePrice(request.getBasePrice());
        product.setCurrency(request.getCurrency().toUpperCase());
        product.setStatus(ProductStatus.ACTIVE);
        product = productRepository.save(product);

        assignCategories(product.getId(), request.getCategoryIds(), request.getPrimaryCategoryId());

        ProductInventory inventory = new ProductInventory();
        inventory.setProductId(product.getId());
        inventory.setOnHandQuantity(
                request.getInitialOnHandQuantity() != null ? request.getInitialOnHandQuantity() : 0);
        inventory.setReservedQuantity(0);
        productInventoryRepository.save(inventory);

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("productId", product.getId().toString());
        payload.put("sku", product.getSku());
        payload.put("status", product.getStatus().name());
        outboxWriter.write(AGG_PRODUCT, product.getId(), "ProductCreated.v1", payload);

        return assemble(product);
    }

    @Override
    @Transactional
    public ProductResponseDTO update(UUID id, ProductUpdateRequestDTO request) {
        Product product = requireActiveOrInactive(id);
        if (product.getVersion() != request.getExpectedVersion()) {
            throw new ConflictException("OPTIMISTIC_LOCK",
                    "Product was modified concurrently. Retry with the latest version.");
        }
        if (request.getName() != null) {
            product.setName(request.getName());
        }
        if (request.getDescription() != null) {
            product.setDescription(request.getDescription());
        }
        if (request.getBrand() != null) {
            product.setBrand(request.getBrand());
        }
        if (request.getBasePrice() != null) {
            product.setBasePrice(request.getBasePrice());
        }
        if (request.getCurrency() != null) {
            product.setCurrency(request.getCurrency().toUpperCase());
        }
        if (request.getStatus() != null) {
            product.setStatus(request.getStatus());
        }
        if (request.getCategoryIds() != null || request.getPrimaryCategoryId() != null) {
            assignCategories(product.getId(), request.getCategoryIds(), request.getPrimaryCategoryId());
        }
        product = productRepository.save(product);

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("productId", product.getId().toString());
        payload.put("status", product.getStatus().name());
        outboxWriter.write(AGG_PRODUCT, product.getId(), "ProductUpdated.v1", payload);

        return assemble(product);
    }

    @Override
    @Transactional
    public void softDelete(UUID id) {
        Product product = requireActiveOrInactive(id);
        product.setStatus(ProductStatus.DISCONTINUED);
        product.setDeletedAt(java.time.Instant.now());
        productRepository.save(product);

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("productId", product.getId().toString());
        outboxWriter.write(AGG_PRODUCT, product.getId(), "ProductDeleted.v1", payload);
    }

    // ---------- Queries ----------

    @Override
    @Transactional(readOnly = true)
    public ProductResponseDTO getActive(UUID id) {
        Product product = productRepository.findByIdAndDeletedAtIsNull(id)
                .filter(p -> p.getStatus() == ProductStatus.ACTIVE)
                .orElseThrow(() -> new NotFoundException("PRODUCT_NOT_FOUND", "Product not found: " + id));
        return assemble(product);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProductResponseDTO> searchActive(String query, UUID categoryId, BigDecimal minPrice,
                                                 BigDecimal maxPrice, Pageable pageable) {
        if (minPrice != null && maxPrice != null && minPrice.compareTo(maxPrice) > 0) {
            throw new BadRequestException("INVALID_PRICE_RANGE", "minPrice must be <= maxPrice");
        }
        Pageable sanitized = PageableUtils.sanitizeProductPageable(pageable);

        Specification<Product> spec = ProductSpecifications.notDeleted()
                .and(ProductSpecifications.hasStatus(ProductStatus.ACTIVE));
        if (query != null && !query.isBlank()) {
            spec = spec.and(ProductSpecifications.nameOrDescriptionContains(query));
        }
        if (categoryId != null) {
            spec = spec.and(ProductSpecifications.inCategory(categoryId));
        }
        if (minPrice != null) {
            spec = spec.and(ProductSpecifications.priceGreaterThanOrEqual(minPrice));
        }
        if (maxPrice != null) {
            spec = spec.and(ProductSpecifications.priceLessThanOrEqual(maxPrice));
        }
        return productRepository.findAll(spec, sanitized).map(this::assemble);
    }

    // ---------- Images ----------

    @Override
    @Transactional
    public ImageResponseDTO addImage(UUID productId, ImageCreateRequestDTO request) {
        requireActiveOrInactive(productId);
        if (request.isPrimary()) {
            clearPrimaryImages(productId);
        }
        ProductImage image = new ProductImage();
        image.setProductId(productId);
        image.setUrl(request.getUrl());
        image.setAltText(request.getAltText());
        image.setDisplayOrder(request.getDisplayOrder());
        image.setPrimary(request.isPrimary());
        return ProductMapper.toImageResponse(productImageRepository.save(image));
    }

    @Override
    @Transactional
    public ImageResponseDTO updateImage(UUID productId, UUID imageId, ImageUpdateRequestDTO request) {
        ProductImage image = productImageRepository.findById(imageId)
                .filter(img -> img.getProductId().equals(productId))
                .orElseThrow(() -> new NotFoundException("IMAGE_NOT_FOUND", "Image not found: " + imageId));
        if (request.getUrl() != null) {
            image.setUrl(request.getUrl());
        }
        if (request.getAltText() != null) {
            image.setAltText(request.getAltText());
        }
        if (request.getDisplayOrder() != null) {
            image.setDisplayOrder(request.getDisplayOrder());
        }
        if (Boolean.TRUE.equals(request.getPrimary())) {
            clearPrimaryImages(productId);
            image.setPrimary(true);
        } else if (Boolean.FALSE.equals(request.getPrimary())) {
            image.setPrimary(false);
        }
        return ProductMapper.toImageResponse(productImageRepository.save(image));
    }

    @Override
    @Transactional
    public void deleteImage(UUID productId, UUID imageId) {
        ProductImage image = productImageRepository.findById(imageId)
                .filter(img -> img.getProductId().equals(productId))
                .orElseThrow(() -> new NotFoundException("IMAGE_NOT_FOUND", "Image not found: " + imageId));
        productImageRepository.delete(image);
    }

    // ---------- Helpers ----------

    private void assignCategories(UUID productId, List<UUID> categoryIds, UUID primaryCategoryId) {
        if (categoryIds == null || categoryIds.isEmpty()) {
            throw new BadRequestException("AT_LEAST_ONE_CATEGORY", "at least one categoryId is required");
        }
        if (primaryCategoryId == null || !categoryIds.contains(primaryCategoryId)) {
            throw new BadRequestException("INVALID_PRIMARY_CATEGORY",
                    "primaryCategoryId must be one of the provided categoryIds");
        }
        Set<UUID> distinct = new LinkedHashSet<>(categoryIds);
        for (UUID categoryId : distinct) {
            if (!categoryRepository.existsById(categoryId)) {
                throw new BadRequestException("CATEGORY_NOT_FOUND", "Category not found: " + categoryId);
            }
        }
        productCategoryRepository.deleteByIdProductId(productId);
        productCategoryRepository.flush();
        for (UUID categoryId : distinct) {
            ProductCategory pc = new ProductCategory();
            pc.setId(new ProductCategoryId(productId, categoryId));
            pc.setPrimary(categoryId.equals(primaryCategoryId));
            productCategoryRepository.save(pc);
        }
    }

    private void clearPrimaryImages(UUID productId) {
        List<ProductImage> current = productImageRepository.findByProductIdAndPrimaryTrue(productId);
        current.forEach(img -> img.setPrimary(false));
        productImageRepository.saveAll(current);
    }

    private Product requireActiveOrInactive(UUID id) {
        return productRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new NotFoundException("PRODUCT_NOT_FOUND", "Product not found: " + id));
    }

    private ProductResponseDTO assemble(Product product) {
        List<ProductCategory> pcs = productCategoryRepository.findByIdProductId(product.getId());
        List<UUID> categoryIds = pcs.stream().map(pc -> pc.getId().getCategoryId()).toList();
        UUID primary = pcs.stream()
                .filter(ProductCategory::isPrimary)
                .map(pc -> pc.getId().getCategoryId())
                .findFirst()
                .orElse(null);
        List<ProductImage> images = productImageRepository.findByProductIdOrderByDisplayOrderAsc(product.getId());
        Integer available = productInventoryRepository.findById(product.getId())
                .map(ProductInventory::getAvailableQuantity)
                .orElse(null);
        return ProductMapper.toResponse(product, categoryIds, primary, images, available);
    }
}
