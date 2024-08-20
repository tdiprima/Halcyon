// Helper function to execute SPARQL queries
function executeSparqlQuery(query, token, isUpdate = false) {
  const endpoint = "https://beak.bmi.stonybrook.edu:8889/rdf";

  const options = {
    method: 'POST',
    headers: {
      'Content-Type': isUpdate ? 'application/sparql-update' : 'application/sparql-query',
      'Authorization': `Bearer ${token}`
    },
    body: query
  };

  // Log the query to check its structure
  // console.log("SPARQL Query:", query);

  return fetch(endpoint, options)
    .then(response => {
      // console.log("Response Headers:", response.headers);
      if (!response.ok) {
        throw new Error(`SPARQL query failed: ${response.statusText}`);
      }
      return response.text();
    })
    .catch(error => {
      console.error('Error executing SPARQL query:', error);
      throw error;
    });
}

// Function to set the annotation label
export function setAnnotationLabel(rdfSubject, newName) {
  const token = window.token;

  const sparqlQuery = `
PREFIX sdo: <https://schema.org/>
PREFIX hal: <https://halcyon.is/ns/>
DELETE {
  GRAPH hal:CollectionsAndResources {
    <${rdfSubject}> sdo:name ?oldname
  }
}
INSERT {
  GRAPH hal:CollectionsAndResources {
    <${rdfSubject}> sdo:name "${newName}"
  }
}
WHERE {
  GRAPH hal:CollectionsAndResources {
    <${rdfSubject}> a hal:Annotation .
    OPTIONAL { <${rdfSubject}> sdo:name ?oldname }
  }
}`;

  return executeSparqlQuery(sparqlQuery, token, true)
    .then(result => {
      console.log('Annotation label set successfully:', newName);
    })
    .catch(error => {
      console.error('Error setting annotation label:', error);
    });
}

// Function to get the annotation label
export function getAnnotationLabel(rdfSubject) {
  const token = window.token;

  const sparqlQuery = `
PREFIX sdo: <https://schema.org/>
PREFIX hal: <https://halcyon.is/ns/>
SELECT ?name WHERE {
  GRAPH hal:CollectionsAndResources {
    <${rdfSubject}> sdo:name ?name
  }
}`;

  return executeSparqlQuery(sparqlQuery, token)
    .then(result => {
      // console.log("Raw SPARQL Query Result:", result);
      // Parse the result as JSON
      let data;
      try {
        data = JSON.parse(result);
      } catch (error) {
        console.error('Error parsing JSON:', error);
        throw new Error('Failed to parse SPARQL result as JSON.');
      }

      // Extract the "name" value from the JSON response
      const bindings = data.results.bindings;
      if (bindings.length > 0 && bindings[0].name) {
        const name = bindings[0].name.value;
        console.log('Retrieved annotation label:', name);
        return name;
      } else {
        console.log('No annotation label found.');
        return null;
      }
    })
    .catch(error => {
      console.error('Error retrieving annotation label:', error);
      throw error;
    });
}
