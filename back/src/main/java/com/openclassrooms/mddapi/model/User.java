package com.openclassrooms.mddapi.model;

import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A member of the MDD social network.
 * <p>
 * Email and username are both unique, and both can be used to log in. The password is
 * stored as a BCrypt hash and must never leave the server.
 */
@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
public class User {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "user_id")
	private Long id;

	@Column(nullable = false, unique = true)
	private String email;

	@Column(nullable = false, unique = true, length = 50)
	private String username;

	/** BCrypt hash of the password. */
	@Column(nullable = false)
	private String password;

	/** Topics the user follows; their posts make up the user's feed. */
	@ManyToMany
	@JoinTable(
			name = "subscriptions",
			joinColumns = @JoinColumn(name = "user_id"),
			inverseJoinColumns = @JoinColumn(name = "topic_id"))
	private Set<Topic> subscriptions = new HashSet<>();
}
