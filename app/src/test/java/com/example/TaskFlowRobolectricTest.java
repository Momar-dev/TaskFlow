package com.example;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import androidx.test.core.app.ApplicationProvider;

import com.example.db.DatabaseHelper;
import com.example.db.TaskRepository;
import com.example.db.UserRepository;
import com.example.model.Task;
import com.example.model.User;
import com.example.security.PasswordHasher;
import com.example.session.SessionManager;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Comprehensive Robolectric tests for TaskFlow.
 * Tests password hashing, registration validation, authentication, session lifecycle,
 * task CRUD, filtering, counter accuracy, and strict multi-user data isolation.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class TaskFlowRobolectricTest {

    private Context context;
    private DatabaseHelper dbHelper;
    private UserRepository userRepository;
    private TaskRepository taskRepository;
    private SessionManager sessionManager;
    private PasswordHasher passwordHasher;

    @Before
    public void setUp() {
        context = ApplicationProvider.getApplicationContext();
        // Use an isolated test database
        dbHelper = new DatabaseHelper(context, "taskflow_test.db");
        userRepository = new UserRepository(dbHelper);
        taskRepository = new TaskRepository(dbHelper);
        sessionManager = new SessionManager(context);
        passwordHasher = new PasswordHasher();

        // Clear session before each test
        sessionManager.logout();
    }

    @After
    public void tearDown() {
        dbHelper.close();
        context.deleteDatabase("taskflow_test.db");
    }

    // 1. Registration succeeds with valid unique credentials
    @Test
    public void testRegistrationSucceedsWithValidCredentials() {
        UserRepository.RegisterStatus status = userRepository.registerUser(
                "alexsmith",
                "alex@example.com",
                "SecurePass123!".toCharArray()
        );
        assertEquals(UserRepository.RegisterStatus.SUCCESS, status);

        User user = userRepository.authenticate("alexsmith", "SecurePass123!".toCharArray());
        assertNotNull(user);
        assertEquals("alexsmith", user.getUsername());
        assertEquals("alex@example.com", user.getEmail());
    }

    // 2. Empty registration fields are rejected
    @Test
    public void testEmptyRegistrationFieldsRejected() {
        assertEquals(UserRepository.RegisterStatus.EMPTY_FIELDS,
                userRepository.registerUser("", "test@example.com", "password123".toCharArray()));
        assertEquals(UserRepository.RegisterStatus.EMPTY_FIELDS,
                userRepository.registerUser("testuser", "", "password123".toCharArray()));
        assertEquals(UserRepository.RegisterStatus.EMPTY_FIELDS,
                userRepository.registerUser("testuser", "test@example.com", new char[0]));
    }

    // 3. Invalid email formats are rejected
    @Test
    public void testInvalidEmailFormatsRejected() {
        assertEquals(UserRepository.RegisterStatus.INVALID_EMAIL,
                userRepository.registerUser("testuser", "not-an-email", "password123".toCharArray()));
        assertEquals(UserRepository.RegisterStatus.INVALID_EMAIL,
                userRepository.registerUser("testuser", "@missinguser.com", "password123".toCharArray()));
    }

    // 4. Password mismatch / short password is rejected
    @Test
    public void testShortPasswordRejected() {
        assertEquals(UserRepository.RegisterStatus.SHORT_PASSWORD,
                userRepository.registerUser("testuser", "user@example.com", "short".toCharArray()));
    }

    // 5. Duplicate accounts (username or email) are rejected
    @Test
    public void testDuplicateAccountsRejected() {
        UserRepository.RegisterStatus first = userRepository.registerUser(
                "duplicateUser",
                "unique@example.com",
                "validPassword123".toCharArray()
        );
        assertEquals(UserRepository.RegisterStatus.SUCCESS, first);

        // Duplicate username with different email
        UserRepository.RegisterStatus duplicateUsername = userRepository.registerUser(
                "duplicateUser",
                "other@example.com",
                "validPassword123".toCharArray()
        );
        assertEquals(UserRepository.RegisterStatus.USERNAME_TAKEN, duplicateUsername);

        // Duplicate email with different username
        UserRepository.RegisterStatus duplicateEmail = userRepository.registerUser(
                "otherUsername",
                "unique@example.com",
                "validPassword123".toCharArray()
        );
        assertEquals(UserRepository.RegisterStatus.EMAIL_TAKEN, duplicateEmail);
    }

    // 6. Passwords are never stored in plaintext
    @Test
    public void testPasswordsNeverStoredInPlaintext() {
        String rawPassword = "SuperSecretPassword123!";
        userRepository.registerUser("plainCheck", "plain@example.com", rawPassword.toCharArray());

        SQLiteDatabase db = dbHelper.getReadableDatabase();
        try (Cursor cursor = db.query(DatabaseHelper.TABLE_USERS,
                new String[]{DatabaseHelper.COL_USER_PASSWORD_HASH},
                DatabaseHelper.COL_USER_USERNAME + " = ?",
                new String[]{"plainCheck"}, null, null, null)) {

            assertTrue(cursor.moveToFirst());
            String storedHash = cursor.getString(0);
            assertNotEquals(rawPassword, storedHash);
            assertFalse(storedHash.contains(rawPassword));
        }
    }

    // 7. Correct credentials allow login (by username or email)
    @Test
    public void testCorrectCredentialsAllowLogin() {
        userRepository.registerUser("loginTester", "tester@domain.com", "correctPass123".toCharArray());

        // Login by username
        User userByUsername = userRepository.authenticate("loginTester", "correctPass123".toCharArray());
        assertNotNull(userByUsername);

        // Login by email
        User userByEmail = userRepository.authenticate("tester@domain.com", "correctPass123".toCharArray());
        assertNotNull(userByEmail);
        assertEquals(userByUsername.getId(), userByEmail.getId());
    }

    // 8. Incorrect credentials are rejected
    @Test
    public void testIncorrectCredentialsRejected() {
        userRepository.registerUser("userA", "userA@domain.com", "goodPassword123".toCharArray());

        // Wrong password
        assertNull(userRepository.authenticate("userA", "wrongPassword!".toCharArray()));
        // Nonexistent user
        assertNull(userRepository.authenticate("nonExistentUser", "goodPassword123".toCharArray()));
    }

    // 9. Logout clears authenticated session
    @Test
    public void testLogoutClearsSession() {
        sessionManager.createSession(42L, "alex", "alex@domain.com");
        assertEquals(42L, sessionManager.getUserId());
        assertEquals("alex", sessionManager.getUsername());

        sessionManager.logout();
        assertEquals(-1L, sessionManager.getUserId());
        assertEquals("", sessionManager.getUsername());
    }

    // 10. Tasks persist in SQLite across operations
    @Test
    public void testTasksPersistInDatabase() {
        userRepository.registerUser("taskOwner", "owner@example.com", "password123".toCharArray());
        User user = userRepository.authenticate("taskOwner", "password123".toCharArray());
        assertNotNull(user);

        Task created = taskRepository.addTask(user.getId(), "Persistent Task", "Should stay in DB", Task.PRIORITY_HIGH, 1792999000L);
        assertNotNull(created);

        // Fetch back from DB
        Task fetched = taskRepository.getTaskById(user.getId(), created.getId());
        assertNotNull(fetched);
        assertEquals("Persistent Task", fetched.getTitle());
        assertEquals("Should stay in DB", fetched.getDescription());
        assertEquals(Task.PRIORITY_HIGH, fetched.getPriority());
    }

    // 11. User can add, edit, complete, reopen, and delete tasks
    @Test
    public void testTaskLifecycleCrud() {
        userRepository.registerUser("crudUser", "crud@example.com", "password123".toCharArray());
        User user = userRepository.authenticate("crudUser", "password123".toCharArray());
        assertNotNull(user);
        long userId = user.getId();

        // Add
        Task task = taskRepository.addTask(userId, "Initial Title", "Initial Desc", Task.PRIORITY_LOW, 0);
        assertNotNull(task);
        assertFalse(task.isCompleted());

        // Edit
        boolean editSuccess = taskRepository.updateTask(userId, task.getId(), "Updated Title", "Updated Desc", Task.PRIORITY_HIGH, 123456L);
        assertTrue(editSuccess);
        Task edited = taskRepository.getTaskById(userId, task.getId());
        assertEquals("Updated Title", edited.getTitle());
        assertEquals(Task.PRIORITY_HIGH, edited.getPriority());

        // Complete
        assertTrue(taskRepository.setTaskCompleted(userId, task.getId(), true));
        Task completed = taskRepository.getTaskById(userId, task.getId());
        assertTrue(completed.isCompleted());

        // Reopen
        assertTrue(taskRepository.setTaskCompleted(userId, task.getId(), false));
        Task reopened = taskRepository.getTaskById(userId, task.getId());
        assertFalse(reopened.isCompleted());

        // Delete
        assertTrue(taskRepository.deleteTask(userId, task.getId()));
        assertNull(taskRepository.getTaskById(userId, task.getId()));
    }

    // 12. Dashboard counters update accurately
    @Test
    public void testDashboardCountersAccuracy() {
        userRepository.registerUser("statsUser", "stats@example.com", "password123".toCharArray());
        User user = userRepository.authenticate("statsUser", "password123".toCharArray());
        assertNotNull(user);
        long userId = user.getId();

        TaskRepository.TaskCounts counts0 = taskRepository.getTaskCounts(userId);
        assertEquals(0, counts0.total);
        assertEquals(0, counts0.active);
        assertEquals(0, counts0.completed);

        Task t1 = taskRepository.addTask(userId, "Task 1", "", Task.PRIORITY_MEDIUM, 0);
        Task t2 = taskRepository.addTask(userId, "Task 2", "", Task.PRIORITY_LOW, 0);
        Task t3 = taskRepository.addTask(userId, "Task 3", "", Task.PRIORITY_HIGH, 0);

        TaskRepository.TaskCounts counts1 = taskRepository.getTaskCounts(userId);
        assertEquals(3, counts1.total);
        assertEquals(3, counts1.active);
        assertEquals(0, counts1.completed);

        // Mark t1 and t2 as completed
        taskRepository.setTaskCompleted(userId, t1.getId(), true);
        taskRepository.setTaskCompleted(userId, t2.getId(), true);

        TaskRepository.TaskCounts counts2 = taskRepository.getTaskCounts(userId);
        assertEquals(3, counts2.total);
        assertEquals(1, counts2.active);
        assertEquals(2, counts2.completed);
    }

    // 13. Filters show correct tasks
    @Test
    public void testFiltersShowCorrectTasks() {
        userRepository.registerUser("filterUser", "filter@example.com", "password123".toCharArray());
        User user = userRepository.authenticate("filterUser", "password123".toCharArray());
        assertNotNull(user);
        long userId = user.getId();

        Task activeTask = taskRepository.addTask(userId, "Active 1", "", Task.PRIORITY_MEDIUM, 0);
        Task completedTask = taskRepository.addTask(userId, "Completed 1", "", Task.PRIORITY_LOW, 0);
        taskRepository.setTaskCompleted(userId, completedTask.getId(), true);

        List<Task> allList = taskRepository.getTasks(userId, TaskRepository.Filter.ALL);
        assertEquals(2, allList.size());

        List<Task> activeList = taskRepository.getTasks(userId, TaskRepository.Filter.ACTIVE);
        assertEquals(1, activeList.size());
        assertEquals(activeTask.getId(), activeList.get(0).getId());

        List<Task> completedList = taskRepository.getTasks(userId, TaskRepository.Filter.COMPLETED);
        assertEquals(1, completedList.size());
        assertEquals(completedTask.getId(), completedList.get(0).getId());
    }

    // 15. Mandatory User Isolation: User A cannot view, edit, or delete User B's tasks
    @Test
    public void testStrictUserDataIsolation() {
        userRepository.registerUser("userAlice", "alice@example.com", "password123".toCharArray());
        userRepository.registerUser("userBob", "bob@example.com", "password123".toCharArray());

        User alice = userRepository.authenticate("userAlice", "password123".toCharArray());
        User bob = userRepository.authenticate("userBob", "password123".toCharArray());
        assertNotNull(alice);
        assertNotNull(bob);

        // Alice creates a task
        Task aliceTask = taskRepository.addTask(alice.getId(), "Alice Secret Task", "Confidential", Task.PRIORITY_HIGH, 0);
        assertNotNull(aliceTask);

        // Bob tries to get Alice's task -> must return null
        assertNull(taskRepository.getTaskById(bob.getId(), aliceTask.getId()));

        // Bob tries to list tasks -> must NOT see Alice's task
        List<Task> bobTasks = taskRepository.getTasks(bob.getId(), TaskRepository.Filter.ALL);
        assertEquals(0, bobTasks.size());

        // Bob tries to edit Alice's task -> must fail and return false
        boolean editAttempt = taskRepository.updateTask(bob.getId(), aliceTask.getId(), "Hacked Title", "Hacked Desc", Task.PRIORITY_LOW, 0);
        assertFalse(editAttempt);

        // Verify Alice's task remains untouched
        Task recheckedAliceTask = taskRepository.getTaskById(alice.getId(), aliceTask.getId());
        assertEquals("Alice Secret Task", recheckedAliceTask.getTitle());

        // Bob tries to delete Alice's task -> must fail and return false
        boolean deleteAttempt = taskRepository.deleteTask(bob.getId(), aliceTask.getId());
        assertFalse(deleteAttempt);

        // Verify task still exists for Alice
        assertNotNull(taskRepository.getTaskById(alice.getId(), aliceTask.getId()));
    }

    // 16. SQL injection bypass protection
    @Test
    public void testSqlInjectionBypassPrevented() {
        String injectionUsername = "admin' OR '1'='1";
        String injectionPassword = "' OR '1'='1";

        User injectedUser = userRepository.authenticate(injectionUsername, injectionPassword.toCharArray());
        assertNull(injectedUser);

        // Attempt injection on task query
        List<Task> tasks = taskRepository.getTasks(-1, TaskRepository.Filter.ALL);
        assertEquals(0, tasks.size());
    }
}
