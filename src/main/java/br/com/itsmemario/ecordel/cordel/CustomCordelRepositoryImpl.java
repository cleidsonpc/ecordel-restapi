package br.com.itsmemario.ecordel.cordel;

import br.com.itsmemario.ecordel.author.Author;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import java.math.BigInteger;
import java.util.List;
import java.util.stream.Collectors;


@Repository
@Transactional(readOnly = true)
class CustomCordelRepositoryImpl implements CustomCordelRepository {

    public static final String TAGS = "tags";

    @PersistenceContext
    private EntityManager entityManager;

    private final String FIND_BY_TAGS_SQL = "select distinct(c.id) as id, c.title, c.description, c.xilogravura, a.name, a.email \n" +
            "from cordel_tags ct \n" +
            "join cordel c on c.id = ct.cordel_id \n" +
            "join author a on a.id = c.author_id \n" +
            "where ct.tags in (:tags)\n" +
            "limit :limit offset :offset";

    private final String FIND_BY_TAGS_SQL_COUNT = "select COUNT(distinct(c.id)) \n" +
            "from cordel_tags ct \n" +
            "join cordel c on c.id = ct.cordel_id \n" +
            "join author a on a.id = c.author_id \n" +
            "where ct.tags in (:tags)\n" ;

    @Override
    public Page<CordelView> findByTags(List<String> tags, Pageable pageable) {
        long count = countResults(tags);

        if(count > 0) {
            Query query = entityManager.createNativeQuery(this.FIND_BY_TAGS_SQL);
            query.setParameter(TAGS, tags);
            query.setParameter("limit", pageable.getPageSize());
            query.setParameter("offset", pageable.getOffset());
            List<Object[]> resultList = query.getResultList();
            List<Cordel> cordels = resultList.stream().map(this::buildCordel).collect(Collectors.toList());

            return new PageImpl(cordels, pageable, count);
        }

        return Page.empty(pageable);
    }

    private long countResults(List<String> tags) {
        BigInteger count = (BigInteger) entityManager.createNativeQuery(FIND_BY_TAGS_SQL_COUNT).setParameter(TAGS, tags).getSingleResult();
        return count.longValue();
    }

    private Cordel buildCordel(Object[] fields) {
        Cordel cordel = new Cordel();
        cordel.setId(((BigInteger)fields[0]).longValue());
        cordel.setTitle(String.valueOf(fields[1]));
        cordel.setDescription(String.valueOf(fields[2]));
        cordel.setXilogravura(String.valueOf(fields[3]));
        cordel.setAuthor(buildAuthor(fields[4],fields[5]));
        return cordel;
    }

    private Author buildAuthor(Object name, Object email) {
        Author author = new Author();
        author.setName(String.valueOf(name));
        author.setEmail(String.valueOf(email));
        return author;
    }

}