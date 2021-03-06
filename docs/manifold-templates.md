---
layout: default
---

# ManTL (Manifold Template Language)

ManTL is a lightweight & _type-safe_ template engine for the JVM using [Manifold](http://manifold.systems/).
It is modeled loosely on Java Server Pages (JSP), but is divorced from the Servlet API and thus can be
used in any application environment.

ManTL supports the full Java language, type-safe arguments to templates, type safe inclusion of other templates,
shared layouts for templates and custom base classes for application-specific logic, among other features.

ManTL files have the suffix `mtl`, often optionally preceded by the language that the template is targeting 
(e.g. `index.html.mtl`).

## Table of Contents
- [Installing](#installing)
- [Usage](#usage)
- [Template Syntax](#basic-syntax)
  * [Statements](#statements)
  * [Expressions](#expressions)
  * [Directives](#directives)
  * [Comments](#comments)
- [Directive Keywords](#directive-keywords)
  * [`import`](#-import-)
  * [`extends`](#-extends-)
  * [`include`](#-include-)
    * [Conditional Include](#conditional-include)
  * [`params`](#-params-)
  * [`section`](#-section-)
  * [`layout`](#-layout-)
- [Layouts](#layouts)
  * [Default Layouts](#default-layouts)
  * [Layout Overrides](#layout-overrides)
- [Spark Java Support](#spark)
  * [Tracing](#tracing)
  * [Spark Template Base Class](#spark-template)
  * [Demo](#demo)
  
<a id="installing" class="toc_anchor"></a>

# Installing #

ManTL can be used by simply adding the following dependency to your project:

```xml
    <dependency>
      <groupId>systems.manifold</groupId>
      <artifactId>manifold-templates</artifactId>
      <version>0.1-alpha</version>
    </dependency>
```

# Usage #

Once you have installed ManTL, you can begin using it by placing a new file that ends
a with the `mtl` suffix in your resources directory (nb: not in your source directory).  The file can have any sort of 
string content, as well as [dynamic content](#basic-syntax) and [directives](#directive-keywords) that change how the 
template behaves.

Consider the following Manifold Template named `HelloWorld.mtl`, located in the `resources/templates` directory:

```jsp
Hello World!
```

This template could then be used directly from your java code like so:

```java
import templates.HelloWorld;

...
    public void demo() {
      System.out.println(HelloWorld.render());
    }
...
```

This would have the effect of printing "Hello World" to standard out.

If you wanted to add a parameter to the template, you can use the [`params`](#-params-) directive:

```jsp
<%@ params(String name) %>
Hello ${name}!
```

You could then call the template like so:

```java
import templates.HelloWorld;

...
    public void demo() {
      System.out.println(HelloWorld.render("ManTL"));
    }
...
```

Which would result in printing "Hello ManTL" to standard out.

If you do not wish to materialize the template as a string, you can use the `renderInto()` method to render templates
into any `Appendable` object.  The `renderInto()` method will have the same parameters as `render()` but will take
an additional `Appendable` object and return `void`.

<a id="basic-syntax" class="toc_anchor"></a>

# Basic Syntax #

As with most template engines, a ManTL consist of regular textual content with various scriptlets and
directives interspersed in that content.

<a id="statements" class="toc_anchor"></a>

## Statements ##

ManTL Statements can contain any number of Java language statements, including variable or method declarations.

The syntax of a statement is as follows:
```jsp
<% code fragment %>
```

Note that any text within a statement *must be valid code in Java*. For example, the statement

```jsp
<% System.out.println("Hello") %>
```
will result in the following Java code being generated:
```java
System.out.println("Hello")
```
which will result in a compiler error, since there is no semicolon to end the line.

<a id="expressions" class="toc_anchor"></a>

## Expressions ##

A ManTL expression contains a Java language expression that is evaluated, converted to a String, and
inserted where the expression appears in the ManTL file.

>Because the value of an expression is converted to a String, you can use an expression within a line of text, whether or not it is tagged with HTML, in a JSP file.

>The expression element can contain any expression that is valid according to the Java Language Specification but you cannot use a semicolon to end an expression.

The syntax of an expression is as follows:
```jsp
<%= expression %>
```

Additionally, the following shorthand syntax is also valid:
```jsp
${ expression }
```

For example, you can do the following with expressions:
```jsp
<html>
  <head><title>Expression Example</title></head>
  <body>
    <% int y = 10; %>
    <p style="font-size: ${y}"> The font size of this paragraph is ${y}. </p>
  </body>
</html>
```

The above code will instantiate an `int y`, assign the value of 10 to it,
and evaluate `y` twice: once to give the paragraph `font-size: 10`, and again
within the paragraph block. It will generate the following HTML:

```html
<html>
  <head><title>Expression Example </title></head>
  <body>
      <p style="font-size: 10"> The font size of this paragraph is 10. </p>
  </body>
</html>
```

<a id="comments" class="toc_anchor"></a>

## Comments ##
Comments are blocks of code that the compiler will ignore. They will **not** be generated as comments in the generated Java code.

The syntax of a comment is as follows:
```jsp
<%-- This is a comment --%>
```

<a id="directives" class="toc_anchor"></a>

## Directives ##

Directives are commands that are evaluated by the compiler, and will affect the overall page structure.

The syntax of a directive is as follows:

```jsp
<%@ directive %>
```
Here are the valid types of directives:

| Directive Type | Syntax                                      | Description                                                                         |
|----------------|---------------------------------------------|-------------------------------------------------------------------------------------|
| Import         | `<%@ import package %>`                     | Imports Java packages into the generated Java file                                  |
| Extends         | `<%@ extends superclass %>`                  | Extends a superclass in the generated Java file                                     |
| Params         | `<%@ params your-params-here %>`            | Gives parameters for the template                                                   |
| Include        | `<%@ include otherTemplate %>`              | Include a separate template in the template                                         |
| Section        | `<%@ section mySection(optional-params) %>` | Creates a sub-template within the template, that can be called from other templates |

A more detailed explanation of various directive types [can be found
below.](#directive-types)


<a id="directive-keywords" class="toc_anchor"></a>

# Directive Keywords #

<a id="-import-" class="toc_anchor"></a>

## `import` ##
The `import` keyword is used to import external packages into the generated Java file.

The syntax of the import keyword is as follows:
```jsp
<%@ import [package name] %>
```

For example, you can use the `import` keyword to utilize useful Java packages:
```jsp
<html>
<%@ import java.util.HashSet %>
  <head><title>Import Example</title></head>
  <body>
    <% int y = 10;
       HashSet<Integer> myHashSet = new HashSet<>();
       myHashSet.add(y);
       myHashSet.add(15);
       for(Integer a: myHashSet) { %>
         <p> myHashSet contains ${a}. </p>
       <% } %>
  </body>
</html>
```
The above code will result in the following HTML. Note that the import statements
was necessary to be able to use `java.util.HashSet`.
```html
<html>
  <head><title>Import Example</title></head>
  <body>
    <p> myHashSet contains 10. </p>
    <p> myHashSet contains 15. </p
  </body>
</html>
```

The location of import statements within the template file is irrelevant. Although it is idiomatic to include all imports
at the beginning of the file, imports can be placed anywhere and will not affect the generated file.

<a id="-extends-" class="toc_anchor"></a>

## `extends` ##
The `extends` keyword is used to make a template extend a different base class, which can be used to provide
additional application specific functionality (e.g. Request and Response objects in a web application).

Here is a practical example of the 'extends' keyword being used:
```java
package demo;

import model.Contact;
import manifold.templates.runtime.BaseTemplate;

public class ExampleTemplate extends BaseTemplate {

  public String displayContact(Contact c) {
    if(c.hasName()) {
      return c.getName();
    } else {
      return c.getEmail();
    }
  }

}
```

This allows the developer to render a clean template:

```jsp
<%@ import model.Contact %>
<%@ extends demo.ExampleTemplate %>
<%@ params(Contact c)%>

<div>
    <div>
      Contact
    </div>
    <div>
      ${displayContact(c)}
    </div>
</div>
```

And easily callable:
```jsp
    get("/contact/:id", (req, resp) -> ShowContact.render(Contact.find(req.getParam("id")));
```


<a id="-include-" class="toc_anchor"></a>

## `include` ##

The `include` keyword allows users to insert other templates inside of the given template in a type
safe manner.

The syntax of the include keyword is as follows:
```jsp
<%@ include [template to include] %>
```

For example, consider the following template, `myTemplate.html.mtl`:
```jsp
<% int fontSize = 0; %>
<html>
    <head><title>WHILE LOOP Example</title></head>
    <body>
        <%while ( fontSize <= 3){ %>
            <font color = "green" size = "<%= fontSize %>">
                JSP Tutorial
            </font><br />
            <%fontSize++;%>
        <%}%>
    </body>
</html>
```
We can then include it from another template as such:
```jsp
<%@ include myTemplate %>
```

Both statements will result in the following HTML code:
```html
<html>
    <head><title>WHILE LOOP Example</title></head>
    <body>
            <font color = "green" size = "0">
                ManTL Tutorial
            </font><
            <font color = "green" size = "1">
                ManTL Tutorial
            </font><br />
            <font color = "green" size = "2">
                ManTL Tutorial
            </font><br />
            <font color = "green" size = "3">
                ManTL Tutorial
            </font><br />
    </body>
</html>

```

<a id="conditional-include" class="toc_anchor"></a>

### Conditional Include ###
ManTL supports shorthand for conditional inclusion of templates. The following syntax:
```jsp
  <% if (condition) { %>
    <%@ include myTemplate %>
  <% } %>
```
Can be condensed to the following:
```jsp
<%@ include myTemplate if(condition) %>
```
(Note: In the above, parentheses are optional.)

<a id="-params-" class="toc_anchor"></a>

## `params` ##

The `params` keyword is used to give parameters to a template. It is only allowed
for the outermost class (not within sections) and is generally only useful when creating
templates that are meant to be included in other templates.

The syntax of the `params` command is as follows:
```jsp
<%@ params(your-params-here) %>
```

For example, I can create the template `NameDisplay.html.mtl` as the following:

```jsp
<%@ params(String name) %>
<p>Your name is: ${myName}</p>
```

You can then include it in another template as follows:

```jsp
<html>
    <head><title>PARAMS Example</title></head>
    <body>
      <%@ include NameDisplay("Sally") %>
      <%@ include NameDisplay("Carson Gross") %>
      <%@ include NameDisplay("Edward") %>
      <%@ include NameDisplay("Harika") %>
    </body>
</html>
```

Then, the following HTML will be generated:
```html
<html>
    <head><title>PARAMS Example</title></head>
    <body>
      <p>Your name is: Sally </p>
      <p>Your name is: Carson Gross </p>
      <p>Your name is: Edward </p>
      <p>Your name is: Harika </p>
    </body>
</html>
```

<a id="-section-" class="toc_anchor"></a>

## `section` ##

The `section` keyword will create a subsection of the current template that can
then be added via an `include` keyword in other templates.

The syntax of a `section` block are as follows:
```jsp
  <%@ section sectionName[(symbols-used-in-section)] %>
    SECTION CONTENT HERE
  <%@ end section %>
```
Note that the corresponding `<%@ end section %>` directive must be used - a failure
to do so will result in an error during code generation.

Imports within sections are valid, and will be handled accordingly.

For example, I can create the template `nestedImport.html.mtl` as the following:

```jsp
    <h1>This will make sure that nested imports are handled correctly.</h1>
    <%@ section mySection %>
        <%@ import java.util.* %>
        <% HashSet<Integer> myHashSet = new HashSet<>();
        myHashSet.add(1);
        myHashSet.add(2);
        myHashSet.add(3);
        for(Integer a: myHashSet) { %>
        <h2 style="font-size: ${a}">Font size: ${a}</h2>
        <% } %>
    <%@ end section %>
        <p> The above section should work </p>
```

The above code will generate the following HTML:
```html
    <h1>This will make sure that nested imports are handled correctly.</h1>
    <h2 style="font-size: 1">Font size: 1</h2>
    <h2 style="font-size: 2">Font size: 2</h2>
    <h2 style="font-size: 3">Font size: 3</h2>
    <p> The above section should work </p>
```

Then, I can include `mySection` it in a separate template:
```jsp
    <%@ include nestedImport.mySection %>
```

Which will result in the following HTML:
```html
    <h2 style="font-size: 1">Font size: 1</h2>
    <h2 style="font-size: 2">Font size: 2</h2>
    <h2 style="font-size: 3">Font size: 3</h2>
```


<a id="-layout-" class="toc_anchor"></a>

## `layout` ##

See below.

<a id="layouts" class="toc_anchor"></a>

# Layouts #

Layouts can be made and used with the `content` and `layouts` directives respectively.

The `content` keyword will split the current template into the header and footer
of a layout.

The `layouts` keyword will make the header and footer of the layout frame the
current template. The code from the current template will appear where the
`content` keyword was originally.

Both the `content` directive and `layouts` directive are only valid in the outermost class
(not within sections) and can only appear once in a template.

The `params` directive is not yet supported for a template that contains the `content` keyword.

The syntax of a content template is as follows:
```jsp
  HEADER CONTENT HERE
  <%@ content %>
  FOOTER CONTENT HERE
```

For example, I can create the template `layoutEx.html.mtl` as the following:
```jsp
    </html>
        </body>
            <%@ content %>
        </body>
    </html>
```

And use the layout in the following template:
```jsp
    <%@ layout layoutEx %>
    <h1>This is a template that uses a layout.</h1>
    <h2>The layout directive can appear anywhere in the template.</h2>
```


The above code will generate the following HTML:
```html
    </html>
        </body>
            <h1>This is a template that uses a layout.</h1>
            <h2>The directive can appear anywhere in the template.</h2>
        </body>
    </html>
```

<a id="default-layouts" class="toc_anchor"></a>

## Default Layouts ##

ManTL also supports the ability to set default layouts for templates in a given package via the
`ManifoldTemplates.java` configuration class:

```java
  ManifoldTemplates.setDefaultLayout(MyLayout.asLayout()); //Sets default template for all templates
  ManifoldTemplates.setDefaultLayout("some.package", AnotherLayout.asLayout()) //Sets default templates for all templates in "some.package"
```

By default, more specific layout declarations will take precedence over less
specific ones. For example, templates with a declared layout (using the layout directive)
will use the declared layout rather than any default layout.

Note that we use the generated `asLayout()` static method on layout template
classes.  This can also be useful when you are overriding layouts, as specified below.

<a id="layout-overrides" class="toc_anchor"></a>

## Layout Overrides ##

Sometimes you may want to manually override the layout of a given template in code,
or render a template with no layout.  ManTL classes include two helper methods:
`withoutLayout` and `withLayout(ILayout)` to assist in these cases:

```java
  MyTemplate.withoutLayout().render(); //Renders the template with no layout, regardless of the configuration
  MyTemplate.withLayout(MyLayout.asLayout()).render(); //Renders MyTemplate with the MyLayout layout, regardless of other configuration
```


<a id="spark" class="toc_anchor"></a>

## Spark Java Support ##

ManTL was designed with web frameworks like [Spark](http://sparkjava.com/) in mind.

Below is an example Spark application making use of ManTL:


```java
package app;

import manifold.templates.ManifoldTemplates;

import static spark.Spark.*;

import views.*;
import views.layout.*;

public class WebApp {

  public static void main(String[] args) {

    // Set up the default layout for the application
    ManifoldTemplates.setDefaultLayout(DefaultLayout.asLayout());

    // enable tracing
    ManifoldTemplates.trace();

    // Render the index template
    get("/", (req, resp) -> Index.render("Hello World"));

  }
}
```

Where there are two templates in the `resources` directory: one at `views/Index.html.mtl` and one at 
`views/layouts/DefaultLayout.html.mtl`.  Note that the code takes advantage of the type-safe parameters available
in ManTL.

<a id="spark-template" class="toc_anchor"></a>

### SparkTemplate Base Class ###

Manifold provides a base class, `manifold.templates.sparkjava.SparkTemplate` that can be used via an `@extends` directive
in your templates (or, more commonly, you would extend the class and add your own application functionality).  This
base class provides various convenience methods to get the HTTP Request, Response, etc. and also automatically escapes
all string content for HTML, to prevent malicious user input from causing a security issue in your application.

If you wish you output raw HTML in a template that extends `manifold.templates.sparkjava.SparkTemplate`, you can use the
`raw()` function to do so:

```jsp
  ${raw("<h1>Some Raw HTML</h1>")}
```
<a id="tracing" class="toc_anchor"></a>

### Tracing ###

ManTL supports performance tracing via the following syntax:
```java
  ManifoldTemplates.trace();
```
After invoking the `trace()` method, every following `render()` call will print
the following to console:
```
  - Template [templateName] rendered in [timeToRender] ms
```

<a id="demo" class="toc_anchor"></a>

### Demo ###

A demo spark application can be found here:

[https://github.com/manifold-systems/spark-java-demo](https://github.com/manifold-systems/spark-java-demo)