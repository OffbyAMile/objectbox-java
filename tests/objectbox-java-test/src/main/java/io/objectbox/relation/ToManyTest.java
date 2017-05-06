package io.objectbox.relation;

import org.junit.Test;


import java.util.ArrayList;
import java.util.List;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class ToManyTest extends AbstractRelationTest {

    @Test
    public void testGet() {
        Customer customer = putCustomerWithOrders(2);
        customer = customerBox.get(customer.getId());
        ToMany<Order> toMany = (ToMany<Order>) customer.getOrders();
        assertFalse(toMany.isResolved());
        assertEquals(2, toMany.size());
        assertTrue(toMany.isResolved());

        assertEquals("order1", toMany.get(0).getText());
        assertEquals("order2", toMany.get(1).getText());
    }

    @Test
    public void testReset() {
        Customer customer = putCustomerWithOrders(2);
        customer = customerBox.get(customer.getId());
        ToMany<Order> toMany = (ToMany<Order>) customer.getOrders();
        assertEquals(2, toMany.size());
        putOrder(customer, "order3");
        assertEquals(2, toMany.size());
        toMany.reset();
        assertFalse(toMany.isResolved());
        assertEquals(3, toMany.size());
        assertTrue(toMany.isResolved());
    }

    @Test
    public void testPutNewCustomerWithNewOrders() {
        Customer customer = new Customer();
        testPutCustomerWithOrders(customer, 5, 0);
    }

    @Test
    public void testPutCustomerWithNewOrders() {
        Customer customer = putCustomer();
        testPutCustomerWithOrders(customer, 5, 0);
    }

    @Test
    public void testPutNewCustomerWithNewAndExistingOrders() {
        Customer customer = new Customer();
        testPutCustomerWithOrders(customer, 5, 5);
    }

    @Test
    public void testPutCustomerWithNewAndExistingOrders() {
        Customer customer = putCustomer();
        testPutCustomerWithOrders(customer, 5, 5);
    }

    private void testPutCustomerWithOrders(Customer customer, int countNewOrders, int countExistingOrders) {
        for (int i = 1; i <= countNewOrders; i++) {
            Order order = new Order();
            order.setText("new" + i);
            customer.orders.add(order);
        }
        for (int i = 1; i <= countExistingOrders; i++) {
            customer.orders.add(putOrder(null, "existing" + i));
        }

        ToMany<Order> toMany = (ToMany<Order>) customer.orders;
        assertEquals(countNewOrders + countExistingOrders, toMany.getAddCount());
        customerBox.put(customer);
        assertEquals(1, customer.getId());
        assertEquals(0, toMany.getAddCount());

        for (int i = 1; i <= countNewOrders; i++) {
            assertEquals(countExistingOrders + i, customer.orders.get(i - 1).getId());
        }

        assertEquals(1, customerBox.count());
        assertEquals(countNewOrders + countExistingOrders, orderBox.count());

        for (Order order : customer.orders) {
            assertEquals(customer.getId(), order.getCustomerId());
            assertEquals(customer.getId(), orderBox.get(order.getId()).getCustomerId());
        }
    }

    @Test
    public void testClear() {
        int count = 5;
        Customer customer = putCustomerWithOrders(count);
        ToMany<Order> toMany = (ToMany<Order>) customer.orders;
        assertFalse(toMany.isResolved());

        toMany.clear();
        assertEquals(count, countOrdersWithCustomerId(customer.getId()));
        customerBox.put(customer);
        assertEquals(0, countOrdersWithCustomerId(customer.getId()));
        assertEquals(count, orderBox.count());
        assertEquals(count, countOrdersWithCustomerId(0));
    }

    @Test
    public void testClear_removeFromTargetBox() {
        Customer customer = putCustomerWithOrders(5);
        ToMany<Order> toMany = (ToMany<Order>) customer.orders;
        toMany.setRemoveFromTargetBox(true);
        toMany.clear();
        customerBox.put(customer);
        assertEquals(0, orderBox.count());
    }

    @Test
    public void testRemove() {
        int count = 5;
        Customer customer = putCustomerWithOrders(count);
        ToMany<Order> toMany = (ToMany<Order>) customer.orders;
        Order removed1 = toMany.remove(3);
        assertEquals("order4", removed1.getText());
        Order removed2 = toMany.get(1);
        assertTrue(toMany.remove(removed2));
        customerBox.put(customer);
        assertOrder2And4Removed(count, customer, toMany);
    }

    @Test
    public void testRemoveAll() {
        int count = 5;
        Customer customer = putCustomerWithOrders(count);
        ToMany<Order> toMany = (ToMany<Order>) customer.orders;
        List<Order> toRemove = new ArrayList<>();
        toRemove.add(toMany.get(1));
        toRemove.add(toMany.get(3));
        assertTrue(toMany.removeAll(toRemove));
        customerBox.put(customer);
        assertOrder2And4Removed(count, customer, toMany);
    }

    @Test
    public void testRetainAll() {
        int count = 5;
        Customer customer = putCustomerWithOrders(count);
        ToMany<Order> toMany = (ToMany<Order>) customer.orders;
        List<Order> toRetain = new ArrayList<>();
        toRetain.add(toMany.get(0));
        toRetain.add(toMany.get(2));
        toRetain.add(toMany.get(4));
        assertTrue(toMany.retainAll(toRetain));
        customerBox.put(customer);
        assertOrder2And4Removed(count, customer, toMany);
    }

    @Test
    public void testSet() {
        int count = 5;
        Customer customer = putCustomerWithOrders(count);
        ToMany<Order> toMany = (ToMany<Order>) customer.orders;
        Order order1 = new Order();
        order1.setText("new1");
        assertEquals("order2", toMany.set(1, order1).getText());
        Order order2 = putOrder(null, "new2");
        assertEquals("order4", toMany.set(3, order2).getText());
        assertEquals(count + 1, orderBox.count());
        customerBox.put(customer);
        assertEquals(count + 2, orderBox.count());

        toMany.reset();
        assertEquals(5, toMany.size());
        assertEquals("order1", toMany.get(0).getText());
        assertEquals("order3", toMany.get(1).getText());
        assertEquals("order5", toMany.get(2).getText());
        assertEquals("new2", toMany.get(3).getText());
        assertEquals("new1", toMany.get(4).getText());
    }

    private void assertOrder2And4Removed(int count, Customer customer, ToMany<Order> toMany) {
        assertEquals(count - 2, countOrdersWithCustomerId(customer.getId()));
        assertEquals(count, orderBox.count());
        assertEquals(2, countOrdersWithCustomerId(0));

        toMany.reset();
        assertEquals(3, toMany.size());
        assertEquals("order1", toMany.get(0).getText());
        assertEquals("order3", toMany.get(1).getText());
        assertEquals("order5", toMany.get(2).getText());
    }

    @Test
    public void testAddRemoved() {
        int count = 5;
        Customer customer = putCustomerWithOrders(count);
        ToMany<Order> toMany = (ToMany<Order>) customer.orders;
        Order order = toMany.get(2);
        assertTrue(toMany.remove(order));
        assertTrue(toMany.add(order));
        assertTrue(toMany.remove(order));
        assertTrue(toMany.add(order));
        assertEquals(0, toMany.getRemoveCount());
        assertEquals(1, toMany.getAddCount());
        customerBox.put(customer);
        assertEquals(count, orderBox.count());
    }

    private long countOrdersWithCustomerId(long customerId) {
        return orderBox.query().equal(Order_.customerId, customerId).build().count();
    }

    private Customer putCustomerWithOrders(int orderCount) {
        Customer customer = putCustomer();
        for (int i = 1; i <= orderCount; i++) {
            putOrder(customer, "order" + i);
        }
        return customer;
    }
}
