describe('MDD application', () => {
  it('loads the Angular application', () => {
    cy.visit('/');
    cy.get('app-root').should('exist');
    cy.title().should('eq', 'MDD');
  });
});
