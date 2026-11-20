# Python coding standards

- Keep all imports at the top of each `.java` file. Never use fully qualified class names inline as a workaround for import conflicts. Resolve circular dependencies through proper package design and dependency inversion.
- Keep each method to 30 lines or fewer. Split longer implementations into small, intention-revealing private methods. Prefer the "functions as comments" approach: method names should communicate the steps and make comments that merely narrate code unnecessary.
- Put each public class, interface, enum, and record in its own `.java` file. Use `PascalCase` for filenames, matching the primary type name.
- Use explicit Java types and avoid raw types. Prefer generics, immutable DTOs, Java records where appropriate, and strongly typed method parameters and return values.
- Follow standard Java naming conventions: `camelCase` for methods and variables, `PascalCase` for classes and interfaces, and `UPPER_SNAKE_CASE` for constants. Use packages in lowercase.
- Keep comments focused on why a non-obvious decision exists. Use Javadoc for public APIs, classes, and methods where additional context is useful.
- Throw specific exceptions and preserve useful error context. Use centralized exception handling where appropriate. Never catch and silently discard exceptions.
- Follow the repository's Checkstyle or Spotless configuration for imports, linting, and formatting. Use consistent Java formatting and a 100-character line length.
- Prefer constructor-based dependency injection over field injection. Use final fields for immutable dependencies.
- Follow SOLID principles and keep classes focused on a single responsibility.
- Prefer composition over inheritance and avoid unnecessary abstractions.
- Use the "functions as comments" approach: method names should describe the implementation steps clearly enough that comments explaining what the code does are unnecessary.