# Practical Examples & Common Scenarios

## 🎯 Example 1: Testing Homepage Performance

### Scenario
Test the homepage with moderate load:

```bash
# Run with 100 users over 2 minutes
COMBINED_USERS=100 COMBINED_DURATION_SECS=120 ./scripts/run_combined.sh
```

### Expected Results
```
Gatling:
- Request count: ~2500
- Mean response: <500ms
- Error rate: 0%

Lighthouse:
- Performance: >80
- LCP: <2.5s
- FID: <100ms
```

## 🎯 Example 2: High Load Testing

### Scenario
Stress test with heavy load:

```bash
# Run with 2500 users over 5 minutes
COMBINED_USERS=2500 COMBINED_DURATION_SECS=300 ./scripts/run_combined.sh
```

### What to Watch
1. Response time degradation
2. Error rate increase
3. Server resource usage
4. Lighthouse score changes

## 🎯 Example 3: Individual Page Testing

### Scenario
Test specific pages with different loads:

```bash
# Heavy load on home, medium on others
HOME_USERS=50 HOME_DURATION_SECS=120 \
BLOG_USERS=20 BLOG_DURATION_SECS=120 \
CART_USERS=10 CART_DURATION_SECS=120 \
./scripts/run_individual.sh
```

### Analysis Points
1. Compare page performances
2. Identify bottlenecks
3. Check resource usage patterns

## 📊 Common Patterns & Solutions

### Pattern 1: Response Time Spikes
```
Symptoms:
- Sudden jumps in response time
- Periodic spikes
- Increasing trend

Solutions:
1. Check database queries
2. Review caching
3. Monitor external services
```

### Pattern 2: Degrading Lighthouse Scores
```
Symptoms:
- Lower performance scores
- Increasing LCP
- Poor CLS

Solutions:
1. Optimize images
2. Reduce JavaScript
3. Implement lazy loading
```

### Pattern 3: Error Rate Increases
```
Symptoms:
- Growing KO count
- Timeout errors
- Connection failures

Solutions:
1. Scale infrastructure
2. Implement retry logic
3. Add circuit breakers
```

## 🔄 Workflow Examples

### Daily Performance Check
```bash
# Morning check
./scripts/run_combined.sh && \
./scripts/collect_json.sh && \
cd playwright && npm run lh:run && npm run lh:aggregate
```

### Pre-Release Testing
```bash
# Clean old results
rm -rf target/gatling/* reports/run-* reports/gatling-json/*

# Run comprehensive test
COMBINED_USERS=2500 COMBINED_DURATION_SECS=300 ./scripts/run_combined.sh

# Generate reports
./scripts/collect_json.sh
cd playwright && npm run lh:run && npm run lh:aggregate
```

### Continuous Monitoring
```bash
# Run every hour
while true; do
  ./scripts/run_combined.sh
  sleep 3600
done
```

## 🎓 Learning Exercises

### Exercise 1: Basic Load Test
1. Run with 100 users
2. Analyze response times
3. Check Lighthouse scores
4. Document findings

### Exercise 2: Compare Pages
1. Test homepage vs cart
2. Compare performance
3. Identify differences
4. Suggest improvements

### Exercise 3: Find Bottlenecks
1. Increase load gradually
2. Monitor metrics
3. Find breaking point
4. Document limits

## 🔍 Troubleshooting Guide

### Problem: High Response Times
```
Check:
1. Database queries
2. External services
3. Cache hit rates
4. Server resources
```

### Problem: Poor Lighthouse Scores
```
Check:
1. Image sizes
2. JavaScript bundles
3. Render-blocking resources
4. Server-side rendering
```

### Problem: Error Rates
```
Check:
1. Server logs
2. Network issues
3. Resource limits
4. Error handling
```

## 📈 Optimization Workflow

1. **Baseline Testing**
   ```bash
   # Get initial metrics
   ./scripts/run_combined.sh
   ```

2. **Identify Issues**
   - Review Gatling reports
   - Check Lighthouse scores
   - List problems found

3. **Make Changes**
   - Optimize code
   - Adjust configurations
   - Update resources

4. **Verify Improvements**
   ```bash
   # Run tests again
   ./scripts/run_combined.sh
   # Compare results
   ```

5. **Document Changes**
   - Record metrics
   - Note improvements
   - Plan next steps
