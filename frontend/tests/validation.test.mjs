import test from 'node:test';
import assert from 'node:assert/strict';
import {validateTicket} from '../src/validation.ts';

test('ordinary questions can be submitted without an order', () => {
  assert.equal(validateTicket({orderId:'',kind:'QUESTION',subject:'商品咨询',description:'需要说明书'}),'');
});
test('after-sales requests require an order', () => {
  for (const kind of ['REFUND','EXCHANGE','REPAIR'])
    assert.match(validateTicket({orderId:'',kind,subject:'售后',description:'说明'}),/关联订单/);
});
test('whitespace and oversized fields are rejected', () => {
  assert.match(validateTicket({orderId:'',kind:'QUESTION',subject:'  ',description:'说明'}),/空格/);
  assert.match(validateTicket({orderId:'',kind:'QUESTION',subject:'字'.repeat(121),description:'说明'}),/120/);
});
test('unknown service kinds are not silently accepted', () => {
  assert.match(validateTicket({orderId:'order-1',kind:'PAY_NOW',subject:'标题',description:'说明'}),/有效/);
});
