# JSONPath

A simple authentication node for ForgeRock's [Identity Platform][forgerock_platform] 7.4.0 and above. This node... **SHORT DESCRIPTION HERE**

## Inputs

Key in shared state that's value is a JSON Object

Example

There is a JSON value in the shared state

In the mappings of the JSON Path Node <br>

The key will be the new shared state item name

The value is the JSON Path (e.g. myJSONExample.$..book.length())


## Configuration
<table>
<thead>
    <th>Property</th>
    <th>Usage</th>
</thead>
<tr>
<td>Mapping</td>
<td>Key is new shared state key, and the value is to JSONPath</td>

</tr>
</table>

## Outputs

Key in Shared State with the value of the Json Path expression

## Outcomes
`Success`

Successfully created a new value in the Shared State with the returned value of the JSON Path expression


`Error`

An error occurred causing the request to fail. Check the response code, response body, or logs to see more details of the error. 
## Examples

![ScreenShot](./example.png)



