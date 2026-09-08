package com.app.features.post.catalogpost.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

import com.app.core.constant.DefaultRoleConstants;
import com.app.core.constant.PermissionConstants;
import com.app.core.enums.RecordStatus;
import com.app.features.post.catalogpost.entity.CatalogAttributeEntity;
import com.app.features.post.catalogpost.entity.CatalogAttributeOptionEntity;
import com.app.features.post.catalogpost.entity.CatalogCategoryAttributeEntity;
import com.app.features.post.catalogpost.entity.CatalogCategoryEntity;
import com.app.features.post.catalogpost.entity.CatalogOfferAttributeValueEntity;
import com.app.features.post.catalogpost.entity.CatalogOfferEntity;
import com.app.features.post.catalogpost.entity.CatalogPostAttributeValueEntity;
import com.app.features.post.catalogpost.entity.CatalogPostEntity;
import com.app.features.post.catalogpost.entity.PostCatalogReferenceEntity;
import com.app.features.post.catalogpost.enums.CatalogAttributeValueType;
import com.app.features.post.catalogpost.enums.CatalogReferenceType;
import com.app.features.post.entity.PostEntity;
import com.app.features.post.enums.PostType;
import com.app.features.post.repository.PostRepository;
import com.app.features.rbac.entity.RoleEntity;
import com.app.features.rbac.repository.RoleRepository;
import com.app.features.user.entity.UserBaseEntity;
import com.app.features.user.repository.UserBaseRepository;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:catalog-foundation;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.flyway.placeholders.bootstrapAdminPasswordHash={noop}test-password",
        "app.auth.credential-hash-secret=0123456789abcdef0123456789abcdef",
        "app.jwt.secret-key=0123456789abcdef0123456789abcdef",
        "app.auth.cookie.secure=false",
        "app.media.storage-path=target/test-data/catalog-foundation-media",
        "app.ai.search.index-directory=target/test-data/catalog-foundation-index",
        "org.jobrunr.background-job-server.enabled=false",
        "org.jobrunr.dashboard.enabled=false"
})
@Transactional
class CatalogPersistenceFoundationTests {

    @Autowired
    private UserBaseRepository userBaseRepo;

    @Autowired
    private PostRepository postRepo;

    @Autowired
    private RoleRepository roleRepo;

    @Autowired
    private CatalogPostRepository catalogPostRepo;

    @Autowired
    private CatalogOfferRepository catalogOfferRepo;

    @Autowired
    private CatalogPostAttributeValueRepository catalogPostAttributeValueRepo;

    @Autowired
    private CatalogOfferAttributeValueRepository catalogOfferAttributeValueRepo;

    @Autowired
    private CatalogCategoryRepository catalogCategoryRepo;

    @Autowired
    private CatalogCategoryAttributeRepository catalogCategoryAttributeRepo;

    @Autowired
    private CatalogAttributeRepository catalogAttributeRepo;

    @Autowired
    private CatalogAttributeOptionRepository catalogAttributeOptionRepo;

    @Autowired
    private PostCatalogReferenceRepository postCatalogReferenceRepo;

    @Test
    void migratesSupplierPermissions() {
        RoleEntity supplier = roleRepo.findByKey(DefaultRoleConstants.SUPPLIER)
                .orElseThrow();
        Set<String> permissionKeys = supplier.getPermissions().stream()
                .map(permission -> permission.getKey())
                .collect(java.util.stream.Collectors.toSet());

        assertThat(permissionKeys).containsExactlyInAnyOrder(
                PermissionConstants.CATALOG_CREATE,
                PermissionConstants.CATALOG_VIEW_OWN,
                PermissionConstants.CATALOG_UPDATE_OWN,
                PermissionConstants.MEDIA_VIEW_OWN,
                PermissionConstants.MEDIA_MANAGE_OWN);
    }

    @Test
    void persistsAndLoadsCatalogAggregateFoundation() {
        UserBaseEntity author = userBaseRepo.findByEmail("system@admin.com")
                .orElseThrow();

        CatalogCategoryEntity category = new CatalogCategoryEntity();
        category.setName("Shoes");
        category.setSlug("shoes");
        category.setSortOrder(0);
        category.setStatus(RecordStatus.ACTIVE);
        catalogCategoryRepo.saveAndFlush(category);

        CatalogAttributeEntity color = new CatalogAttributeEntity();
        color.setKey("color");
        color.setName("Color");
        color.setValueType(CatalogAttributeValueType.OPTION);
        color.setSearchable(true);
        color.setStatus(RecordStatus.ACTIVE);
        catalogAttributeRepo.saveAndFlush(color);

        CatalogAttributeOptionEntity white = new CatalogAttributeOptionEntity();
        white.setAttribute(color);
        white.setCode("white");
        white.setLabel("White");
        white.setSortOrder(0);
        white.setStatus(RecordStatus.ACTIVE);
        catalogAttributeOptionRepo.saveAndFlush(white);

        CatalogCategoryAttributeEntity categoryColor =
                new CatalogCategoryAttributeEntity();
        categoryColor.setCategory(category);
        categoryColor.setAttribute(color);
        categoryColor.setRequired(true);
        categoryColor.setFilterable(true);
        categoryColor.setMultipleValuesAllowed(false);
        categoryColor.setCustomOptionAllowed(false);
        categoryColor.setSortOrder(0);
        catalogCategoryAttributeRepo.saveAndFlush(categoryColor);

        PostEntity catalogRoot = createPost(author, PostType.CATALOG);
        CatalogPostEntity catalogPost = new CatalogPostEntity();
        catalogPost.setPost(catalogRoot);
        catalogPost.setCategory(category);
        catalogPost.setTitle("White shoes");
        catalogPost.setDescription("Catalog persistence test");
        catalogPostRepo.saveAndFlush(catalogPost);

        CatalogPostAttributeValueEntity colorValue =
                new CatalogPostAttributeValueEntity();
        colorValue.setCatalogPost(catalogPost);
        colorValue.setAttribute(color);
        colorValue.setValueType(CatalogAttributeValueType.OPTION);
        colorValue.setOption(white);
        colorValue.setPosition(0);
        catalogPostAttributeValueRepo.saveAndFlush(colorValue);

        CatalogOfferEntity offer = new CatalogOfferEntity();
        offer.setCatalogPost(catalogPost);
        offer.setName("Default offer");
        offer.setDefaultOffer(true);
        offer.setPosition(0);
        offer.setStatus(RecordStatus.ACTIVE);
        catalogOfferRepo.saveAndFlush(offer);

        CatalogOfferAttributeValueEntity price =
                new CatalogOfferAttributeValueEntity();
        price.setCatalogOffer(offer);
        price.setCustomKey("price");
        price.setCustomName("Price");
        price.setValueType(CatalogAttributeValueType.MONEY);
        price.setNumberValue(new BigDecimal("1000000"));
        price.setUnit("VND");
        price.setPosition(0);
        catalogOfferAttributeValueRepo.saveAndFlush(price);

        PostEntity reviewPost = createPost(author, PostType.STANDARD);
        PostCatalogReferenceEntity reference = new PostCatalogReferenceEntity();
        reference.setPost(reviewPost);
        reference.setCatalogPost(catalogPost);
        reference.setReferenceType(CatalogReferenceType.REVIEW);
        reference.setPosition(0);
        postCatalogReferenceRepo.saveAndFlush(reference);

        assertThat(catalogPostRepo.findDetailByPostId(catalogRoot.getId()))
                .isPresent()
                .get()
                .extracting(result -> result.getPost().getType())
                .isEqualTo(PostType.CATALOG);
        assertThat(catalogCategoryRepo.findDetailById(category.getId()))
                .isPresent();
        assertThat(catalogAttributeRepo.findByKey("color")).isPresent();
        assertThat(catalogAttributeOptionRepo
                .findAllByAttribute_Id(
                        color.getId(),
                        PageRequest.of(0, 20))
                .getContent())
                .containsExactly(white);
        assertThat(catalogCategoryAttributeRepo
                .findAllByCategory_IdOrderBySortOrderAsc(category.getId()))
                .containsExactly(categoryColor);
        assertThat(catalogPostAttributeValueRepo
                .findAllByCatalogPost_PostIdOrderByPositionAsc(
                        catalogRoot.getId()))
                .containsExactly(colorValue);
        assertThat(catalogOfferRepo
                .findAllByCatalogPost_PostIdOrderByPositionAsc(
                        catalogRoot.getId()))
                .containsExactly(offer);
        assertThat(catalogOfferAttributeValueRepo
                .findAllByCatalogOffer_IdOrderByPositionAsc(offer.getId()))
                .containsExactly(price);
        assertThat(postCatalogReferenceRepo
                .findAllByPost_IdOrderByPositionAsc(reviewPost.getId()))
                .containsExactly(reference);
    }

    private PostEntity createPost(
            UserBaseEntity author,
            PostType postType) {
        PostEntity post = new PostEntity();
        post.setAuthor(author);
        post.setType(postType);
        return postRepo.saveAndFlush(post);
    }
}
