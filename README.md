# JSONPath

A simple authentication node for ForgeRock's [Identity Platform][forgerock_platform] 7.4.0 and above. This node implements JSON Path filter search to grab values or execute an expression on a JSON Object in Shared State

## Inputs

Key in shared state that's value is a JSON Object

## Compatibility

<table>
  <colgroup>
    <col>
    <col>
  </colgroup>
  <thead>
  <tr>
    <th>Product</th>
    <th>Compatible?</th>
  </tr>
  </thead>
  <tbody>
  <tr>
    <td><p>ForgeRock Identity Cloud</p></td>
    <td><p><span>Yes</span></p></td>
  </tr>
  <tr>
    <td><p>ForgeRock Access Management (self-managed)</p></td>
    <td><p><span>Yes</span></p></td>
  </tr>
  <tr>
    <td><p>ForgeRock Identity Platform (self-managed)</p></td>
    <td><p><span>Yes</span></p></td>
  </tr>
  </tbody>
</table>

## How to use 

To use JSONpath expressions for variable subsitution, use an expression of the form `<shared state variable>.$.<path>`

for example, given a shared state variable called "objectAttributes" containing this JSON data:

```
{
  "username": "bob",
  "firstName": "Bob",
  "lastname": "Fleming",
  "telephoneNumber": "+1(555)1231234",
  "bookingIDs": [ 29872, 23884, 48382 ],
  "membershipTier": "platinum"
}
```

the "firstname" and "lastname" attributes can be selected as `${objectAtttributes.$.firstname}` and `${objectAttributes.$.lastname}` respectively.
The last booking ID can be selected as `${objectAttributes.$.bookingIDs[2]}` or `${objectAttributes.$.bookingIDs[-1]}`.

Similar notation can be used with the JSON Response Handler property to save returned JSON response values to shared state.
For example, if the JSON object above is the response to a REST API call, then a JSON Response Handler configuration with key/value as follows will select the telephone number from the response and save it to shared state variable "phone":

- Key: phone
- Value: $.telephoneNumber

The JSON Response Outcome Handler can be filtered with a suitable JSONpath expression to look for matching responses.
A matching outcome is triggered when the JSONpath expression, when applied to an array containing the JSON response, results in a non-empty array, i.e., the expression finds at least one match.
For example, if the JSON object above is the response to a REST API call, then a JSON Response Outcome Handler configuration with key/value as follows will trigger the "Priority" outcome:

- Key: Priority
- $.[?(@.membershipTier == 'platinum')]

Full details of [JSONpath](https://github.com/json-path/JsonPath/blob/master/README.md) expressions can be found [here](https://github.com/json-path/JsonPath/blob/master/README.md).





## Configuration
<table>
<thead>
    <th>Property</th>
    <th>Usage</th>
</thead>
<tr>
<td>Insert into Shared State</td>
<td>Key - variable name to be inserted into Shared State.<br>
    Value - Value to be inserted into Shared State. Wrap value in "" to insert static value into Shared State. Value can JSON Path value as well
</td>
</tr>
<tr>
<td>JSON Path filter to become outcome</td>
<td>If JSON Path filter matches; The node will go to that outcome. More than one matching JSON Path filter will result in the <code>Error</code> outcome</td>
</tr>
</table>

## Outputs

Key in Shared State with static value or Json Path expression value

## Outcomes

`JSON Path`

JSON Path filter successfully matched

`Next`

Successfully created a new value in the Shared State with the returned value of the JSON Path expression


`Error`

An error occurred causing the Node to fail. Check the logs to see more details of the error. 

## Examples

![ScreenShot](./example.png)



