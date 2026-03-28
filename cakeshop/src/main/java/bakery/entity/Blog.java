package bakery.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.Nationalized;
import java.time.LocalDateTime;

@Entity
@Table(name = "blogs")
@Data
public class Blog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Nationalized
    @Column(nullable = false)
    private String title;

    @Nationalized
    @Column(columnDefinition = "NVARCHAR(MAX)")
    private String summary;

    @Nationalized
    @Column(columnDefinition = "NVARCHAR(MAX)")
    private String content;

    @Column(name = "image", length = 255)
    private String image;

    private LocalDateTime createdAt = LocalDateTime.now();
}