pm.test("Estado de respuesta es 200 OK", function (){
    pm.response.to.have.status(200);
});

pm.test("la estructura principal tiene los tipos correctos", function (){
    const json = pm.response.json();
    pm.expect(json.page).to.be.a('number');
    pm.expect(json.per_page).to.be.a('number');
    pm.expect(json.total).to.be.a('number');
    pm.expect(json.total_pages).to.be.a('number');
    pm.expect(json.data).to.be.a('array');
}
);

pm.test("Cada usuario en data contiene los campos obligatorios", function(){
    const json = pm.response.json();

    json.data.forEach((user) => {
        pm.expect(user.id).to.be.a('number');
        pm.expect(user.email).to.be.a('string')
        pm.expect(user.first_name).to.be.a('string')
        pm.expect(user.last_name).to.be.a('string')
        pm.expect(user.avatar).to.be.a('string')
})
});

// Validar que los IDs dentro del arreglo 'data' sean correlativos empezando desde 1
pm.test("Los IDs de los usuarios coinciden con su posición indexada (id = index + 1)", function () {
    const json = pm.response.json();
    json.data.forEach((user, index) => {
        const esperado = index + 1;
        pm.expect(user.id).to.eql(index + 1);
    });
});