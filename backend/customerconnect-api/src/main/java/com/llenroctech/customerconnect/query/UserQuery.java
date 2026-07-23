package com.llenroctech.customerconnect.query;

public class UserQuery {

    public static final String COUNT_USER_EMAIL_QUERY =
            "SELECT COUNT(*) FROM Users WHERE email = :email";

    public static final String INSERT_USER_QUERY =
            "INSERT INTO Users (first_name, last_name, email, password) " +
                    "VALUES (:firstName, :lastName, :email, :password)";

    public static final String INSERT_ACCOUNT_VERIFICATION_URL_QUERY =
            "INSERT INTO AccountVerifications (user_id, url, `date`) " +
                    "VALUES (:userId, :url, NOW())";

    public static final String SELECT_USER_BY_EMAIL_QUERY =
            "SELECT * FROM Users WHERE email = :email";

    public static final String DELETE_VERIFICATION_CODE_BY_USER_ID =
            """
            DELETE FROM TwoFactorVerifications
            WHERE user_id = :id
            """;

    public static final String INSERT_VERIFICATION_CODE_QUERY =
            """
            INSERT INTO TwoFactorVerifications
                (user_id, code, expiration_date)
            VALUES
                (:userId, :code, :expirationDate)
            """;

    public static final String DELETE_PASSWORD_RESET_BY_USER_ID_QUERY =
            """
            DELETE FROM ResetPasswordVerifications
            WHERE user_id = :userId
            """;

    public static final String INSERT_PASSWORD_RESET_QUERY =
            """
            INSERT INTO ResetPasswordVerifications
                (user_id, url, expiration_date)
            VALUES
                (:userId, :url, :expirationDate)
            """;

    public static final String SELECT_PASSWORD_RESET_VERIFICATION_QUERY =
            """
            SELECT
                verification.user_id,
                verification.expiration_date,
                user.enabled,
                user.non_locked
            FROM ResetPasswordVerifications verification
            INNER JOIN Users user
                ON user.id = verification.user_id
            WHERE verification.url = :url
            """;

    public static final String UPDATE_USER_PASSWORD_QUERY =
            """
            UPDATE Users
            SET password = :password
            WHERE id = :userId
            """;

    public static final String DELETE_PASSWORD_RESET_TOKEN_QUERY =
            """
            DELETE FROM ResetPasswordVerifications
            WHERE user_id = :userId
              AND url = :url
            """;

    public static final String CONSUME_VALID_VERIFICATION_CODE_QUERY =
            """
            DELETE verification
            FROM TwoFactorVerifications verification
            INNER JOIN Users user
                ON user.id = verification.user_id
            WHERE user.email = :email
              AND verification.code = :code
              AND verification.expiration_date > NOW()
            """;
}
